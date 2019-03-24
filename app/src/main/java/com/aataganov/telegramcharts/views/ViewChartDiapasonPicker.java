package com.aataganov.telegramcharts.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.aataganov.telegramcharts.R;
import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.helpers.ListHelper;
import com.aataganov.telegramcharts.helpers.MathHelper;
import com.aataganov.telegramcharts.models.Chart;
import com.aataganov.telegramcharts.utils.ChartHelper;
import com.aataganov.telegramcharts.views.models.ChartDiapason;
import com.aataganov.telegramcharts.views.models.DiapasonPickerSelectedDiapason;
import com.aataganov.telegramcharts.views.models.StepValues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import static com.aataganov.telegramcharts.helpers.Constants.FULL_ALPHA;
import static com.aataganov.telegramcharts.helpers.Constants.HALF_ALPHA;

public class ViewChartDiapasonPicker extends View implements ViewChart.DiapasonPicker {
    private static final String LOG_TAG = ViewChartDiapasonPicker.class.getSimpleName();
    public static final int ANIMATION_FRAME_COUNT = 10;
    public static final int TRANSITION_ANIMATION_FRAME_COUNT = 15;
    public static final float MOVE_SENSITIVITY = 5f;
    int verticalPadding;
    int horizontalPadding;
    int transitionAnimationAlpha = FULL_ALPHA;

    List<Boolean> oldSelection = Collections.emptyList();
    List<Boolean> currentSelection = Collections.emptyList();

    BehaviorSubject<ChartDiapason> selectedDiapasonSubject = BehaviorSubject.create();

    private float startPosition;
    private float touchedAreaStartPosition;
    private boolean animatingTouch = false;
    private boolean animatingTransition = false;
    private TouchedArea touchedArea;
    private long movingStartTime;
    private Long animationProgress;
    private float lastShift = 0;
    private PublishSubject<Float> moveShiftSubject = PublishSubject.create();
//    private BehaviorSubject<>

    private Chart chart;
    private Paint graphPaint = new Paint();
    private Paint diapasonEdgesPaint = new Paint();
    private Paint diapasonSkipPaint = new Paint();
    private StepValues stepValues;
    private DiapasonPickerSelectedDiapason selectedDiapason;
    private CompositeDisposable viewBag = new CompositeDisposable();
    private Disposable touchAnimationDisposable;
    private Disposable transitionAnimationDisposable;
    private boolean isMovingDiapason = false;

    public ViewChartDiapasonPicker(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        subscribeToShiftChanges();
        verticalPadding = context.getResources().getDimensionPixelSize(R.dimen.diapason_selection_vertical_padding);
        horizontalPadding = context.getResources().getDimensionPixelSize(R.dimen.diapason_selection_horizontal_padding);
        selectedDiapason = new DiapasonPickerSelectedDiapason(context.getResources().getDimensionPixelSize(R.dimen.diapason_selection_edge_width), verticalPadding, horizontalPadding);
        stepValues = new StepValues(verticalPadding, horizontalPadding);
        initPaints();
    }

    public ViewChartDiapasonPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ViewChartDiapasonPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(CommonHelper.isDisposed(viewBag)){
            viewBag = new CompositeDisposable();
            subscribeToShiftChanges();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        CommonHelper.unsubscribeDisposeBag(viewBag);
        CommonHelper.unsubscribeDisposable(touchAnimationDisposable);
        CommonHelper.unsubscribeDisposable(transitionAnimationDisposable);
        viewBag = null;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateValues(false);
    }

    public void setChart(Chart chart, List<Boolean> selectionList) {
        oldSelection = null;
        currentSelection = new ArrayList<>(selectionList);
        this.chart = chart;
        updateValues(true);

    }

    private void updateValues(Boolean withTranstion){
        viewBag.add(Single.fromCallable(this::recalculateValues)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if(withTranstion){
                        launchTransitionAnimation();
                    } else {
                        postInvalidate();
                    }
                }, error -> {
                    error.printStackTrace();
                    Log.w(LOG_TAG,"Couldn't update chart values");
                }));
    }

    private void initPaints(){
        graphPaint.setAntiAlias(true);
        graphPaint.setStrokeWidth(3);
        graphPaint.setStyle(Paint.Style.STROKE);
        diapasonEdgesPaint.setStyle(Paint.Style.FILL);
        diapasonEdgesPaint.setColor(Color.DKGRAY);
        diapasonEdgesPaint.setAlpha(HALF_ALPHA);
        diapasonSkipPaint.setStyle(Paint.Style.FILL);
        diapasonSkipPaint.setColor(Color.LTGRAY);
        diapasonSkipPaint.setAlpha(HALF_ALPHA);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        if(chart == null){
            return;
        }
        drawChart(canvas);
        drawDiapasonSkipAreas(canvas);
        drawDiapasonEdges(canvas);
        drawSelectionCircle(canvas);
    }

    private void drawChart(Canvas canvas){
        int graphsSize = chart.getGraphsList().size();
        int fadingAlpha = FULL_ALPHA - transitionAnimationAlpha;
        boolean hasOldSelection = ListHelper.hasItems(oldSelection);
        float yStep = stepValues.calculateTransitionStep(transitionAnimationAlpha);
        for(int index = 0; index < graphsSize; ++index){
            Chart.GraphData graph = chart.getGraphsList().get(index);
            if(currentSelection.get(index)) {
                if(hasOldSelection && oldSelection.get(index)){
                    drawGraph(graph.getColor(), FULL_ALPHA, yStep, canvas, graph.getValues());
                } else {
                    drawGraph(graph.getColor(), transitionAnimationAlpha, yStep, canvas, graph.getValues());
                }
            } else if(hasOldSelection && oldSelection.get(index)){
                drawGraph(graph.getColor(),fadingAlpha, yStep, canvas,graph.getValues());
            }
        }
    }

    private void drawSelectionCircle(Canvas canvas){
        if((!animatingTouch && !isMovingDiapason || touchedArea == TouchedArea.NONE)){
            return;
        }
        canvas.drawCircle(selectedDiapason.getSelectedAreaCenter(touchedArea),stepValues.getyCenter(), calculateSelectionCircleRadius(), diapasonSkipPaint);
    }

    private float calculateSelectionCircleRadius(){
        if(isMovingDiapason && !animatingTouch){
            return stepValues.getyCenter();
        }
        long circlePart = isMovingDiapason ? animationProgress : (ANIMATION_FRAME_COUNT - animationProgress);
        return (stepValues.getyCenter() * circlePart) / (ANIMATION_FRAME_COUNT);
    }

    private void drawGraph(int color, int alpha, float stepY, Canvas canvas, List<Integer> values){
        graphPaint.setColor(color);
        graphPaint.setAlpha(alpha);
        Path path = ChartHelper.buildGraphPath(values, stepValues.getHeightWithoutPadding(), stepValues.getStepX(), stepY);
        path.offset(horizontalPadding,verticalPadding);
        canvas.drawPath(path,graphPaint);
    }

    private void drawDiapasonSkipAreas(Canvas canvas){
        if(!selectedDiapason.needToDrawStartSkip() && !selectedDiapason.needToDrawEndSkip()){
            return;
        }
        Path path = new Path();
        if(selectedDiapason.needToDrawStartSkip()){
            path.addRect(selectedDiapason.getStartSkip(), Path.Direction.CW);
        }
        if(selectedDiapason.needToDrawEndSkip()){
            path.addRect(selectedDiapason.getEndSkip(), Path.Direction.CW);
        }
        path.close();
        canvas.drawPath(path,diapasonSkipPaint);
    }

    private void drawDiapasonEdges(Canvas canvas){
        Path path = new Path();
        path.addRect(selectedDiapason.getStartEdge(), Path.Direction.CW);
        path.addRect(selectedDiapason.getEndEdge(), Path.Direction.CW);
        path.close();
        canvas.drawPath(path,diapasonEdgesPaint);
    }

    private boolean recalculateValues(){
        stepValues.update(chart,this);
        selectedDiapason.update(stepValues,this);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & 255){
            case MotionEvent.ACTION_DOWN:
                onPointerDown(event);
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                onCancelMove();
                return true;
            case MotionEvent.ACTION_MOVE:
                onMove(event);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void onMove(MotionEvent event){
        float originalShift = startPosition - event.getX();
        if(MathHelper.isInRange(originalShift - lastShift, MOVE_SENSITIVITY)){
            return;
        }
        lastShift = originalShift;
        moveShiftSubject.onNext(lastShift);
    }

    private void subscribeToShiftChanges(){
        viewBag.add(moveShiftSubject.subscribeOn(Schedulers.io())
                .map(shift -> selectedDiapason.moveToNewPosition(touchedArea, touchedAreaStartPosition - shift))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if(result){
                        selectedDiapasonSubject.onNext(selectedDiapason.calculateDiapason());
                    }
                    if(result && !animatingTouch){
                        postInvalidate();
                    }
                }, Throwable::printStackTrace));
    }

    public Observable<ChartDiapason> getSelectedDiapasonObservable(){
        return selectedDiapasonSubject.subscribeOn(Schedulers.io());
    }

    private void onPointerDown(MotionEvent event){
        touchedArea = selectedDiapason.getTouchedArea(event.getX());
        if(touchedArea == TouchedArea.NONE){
            isMovingDiapason = false;
            return;
        }
        enterMovingState(event);
        launchTouchAnimation();
    }

    private void onCancelMove(){
        isMovingDiapason = false;
        launchTouchAnimation();
    }

    private void enterMovingState(MotionEvent event){
        isMovingDiapason = true;
        startPosition = event.getX();
        lastShift = 0;
        touchedAreaStartPosition = selectedDiapason.getAreaPosition(touchedArea);
        movingStartTime = System.currentTimeMillis();
    }

    public enum TouchedArea {
        START_EDGE,
        END_EDGE,
        SELECTED_AREA,
        NONE
    }

    private void launchTouchAnimation(){
        animatingTouch = true;
        CommonHelper.unsubscribeDisposable(touchAnimationDisposable);
        touchAnimationDisposable = (Observable.intervalRange(1L, ANIMATION_FRAME_COUNT,0L,10L, TimeUnit.MILLISECONDS,AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    animationProgress = res;
                    postInvalidate();
                }, error -> {
                    animationProgress = -1L;
                    animatingTouch = false;
                    error.printStackTrace();
                }, () -> {
                    animatingTouch = false;
                    }
                ));
    }

    public void setNewSelection(List<Boolean> newSelection){
        oldSelection = currentSelection;
        currentSelection = newSelection;
        stepValues.updateStepY(chart, currentSelection);
        launchTransitionAnimation();
    }

    private void launchTransitionAnimation(){
        animatingTransition = true;
        transitionAnimationAlpha = 0;
        CommonHelper.unsubscribeDisposable(transitionAnimationDisposable);
        transitionAnimationDisposable = (Observable.intervalRange(1L, TRANSITION_ANIMATION_FRAME_COUNT,0L,30L, TimeUnit.MILLISECONDS,Schedulers.computation())
                .map(lvl -> calculateTransitionAnimationAlpha(lvl))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                            transitionAnimationAlpha = res;
                            invalidate();
                        }, error -> {
                            transitionAnimationAlpha = FULL_ALPHA;
                            animatingTransition = false;
                            error.printStackTrace();
                        }, () -> {
                            transitionAnimationAlpha = FULL_ALPHA;
                            animatingTransition = false;
                        }
                ));
    }
    private int calculateTransitionAnimationAlpha(long lvl){
        if(lvl >= TRANSITION_ANIMATION_FRAME_COUNT){
            return FULL_ALPHA;
        }
        return (int) (FULL_ALPHA * lvl / TRANSITION_ANIMATION_FRAME_COUNT);
    }
}

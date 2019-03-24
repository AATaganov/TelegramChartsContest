package com.aataganov.telegramcharts.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.aataganov.telegramcharts.R;
import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.helpers.DateHelper;
import com.aataganov.telegramcharts.helpers.MathHelper;
import com.aataganov.telegramcharts.models.Chart;
import com.aataganov.telegramcharts.utils.ChartHelper;
import com.aataganov.telegramcharts.views.models.ChartDiapason;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import static com.aataganov.telegramcharts.helpers.Constants.FULL_ALPHA;

public class ViewChart extends View {
    public static final int Y_TRANSITION_ANIMATION_FRAME_COUNT = 15;
    public static final int DEFAULT_ANIMATION_FRAME_COUNT = 15;
    public static final long DEFAULT_ANIMATION_STEP = 25L;
    private static final int METRICS_ITEMS_TO_DISPLAY = 5;
    private static final int DATE_ITEMS_TO_DISPLAY = 5;
    private static final String LOG_TAG = ViewChart.class.getSimpleName();

    ChartDiapason currentDiapason;
    ChartDiapason oldDiapason;

    private Chart chart;
    List<Boolean> oldSelection = Collections.emptyList();
    List<Boolean> currentSelection = Collections.emptyList();

    int currentDatesStep = 1;
    int oldDatesStep = 1;

    private Paint graphPaint = new Paint();
    private Paint metricPaint = new Paint();
    private Paint metricTextPaint = new Paint();

    int verticalPadding;
    int metricTextPadding;
    int baseLine = 0;
    int chartHeight = 0;

    int transitionAlphaY = FULL_ALPHA;
    int datesTransitionAlpha = FULL_ALPHA;

    MetricsValues currentMetricsValues;
    MetricsValues oldMetricValues;

    private CompositeDisposable viewBag = new CompositeDisposable();
    private Disposable transitionAnimationDisposable;
    private Disposable yTransitionAnimationDisposable;
    private Disposable datesAnimationDisposable;
    private DiapasonPicker diapasonPicker;
    private Disposable selectedDiapasonDisposable;
    private PublishSubject<Boolean> invalidateRequestsSubject = PublishSubject.create();
    private PublishSubject<Integer> maxYSubject = PublishSubject.create();
    private PublishSubject<Integer> dateStepSubject = PublishSubject.create();

    public ViewChart(Context context) {
        super(context);
        init(context);
    }

    public ViewChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ViewChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        verticalPadding = context.getResources().getDimensionPixelSize(R.dimen.chart_vertical_padding);
        metricTextPadding = context.getResources().getDimensionPixelSize(R.dimen.chart_metric_text_padding);
        initPaints();
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(CommonHelper.isDisposed(viewBag)){
            viewBag = new CompositeDisposable();
        }
        subscribeToInvalidateEvents();
        subscibeToNewMaxYEvents();
        subscribeTonewDatesEvents();
    }

    private void subscribeToInvalidateEvents(){
        viewBag.add(invalidateRequestsSubject
                .throttleLatest(10, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(res -> postInvalidate(), Throwable::printStackTrace));
    }
    private void subscibeToNewMaxYEvents(){
        viewBag.add(maxYSubject
                .throttleLatest(250, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(newMaxY -> {
                    if(currentMetricsValues == null || currentMetricsValues.maxValueY != newMaxY) {
                        oldMetricValues = currentMetricsValues;
                        currentMetricsValues = new MetricsValues(newMaxY, chartHeight);
                        launchMetricTransition();
                    }
                }, Throwable::printStackTrace));
    }
    private void subscribeTonewDatesEvents(){
        viewBag.add(dateStepSubject
                .throttleLatest(250, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(newStep -> {
                    if(currentDatesStep != newStep) {
                        oldDatesStep = currentDatesStep;
                        currentDatesStep = newStep;
                        launchDatesTransition();
                    }
                }, Throwable::printStackTrace));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        CommonHelper.unsubscribeDisposeBag(viewBag);
        CommonHelper.unsubscribeDisposable(transitionAnimationDisposable);
        CommonHelper.unsubscribeDisposable(selectedDiapasonDisposable);
        viewBag = null;
    }


    private void initPaints(){
        graphPaint.setAntiAlias(true);
        graphPaint.setStrokeWidth(3);
        graphPaint.setStyle(Paint.Style.STROKE);

        metricPaint.setAntiAlias(true);
        metricPaint.setColor(Color.LTGRAY);
        metricPaint.setStyle(Paint.Style.STROKE);
        metricPaint.setStrokeWidth(1);

        metricTextPaint.setTextSize(getContext().getResources().getDimensionPixelSize(R.dimen.chart_metric_text_size));
        metricTextPaint.setAntiAlias(true);
        metricTextPaint.setColor(Color.GRAY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        if(chart == null) {
            return;
        }
        ChartDiapason.DrawChartValues chartValues = currentDiapason.getDrawChartValues(getWidth());
        float currentStepY = calculateTransitionY();
        drawMetricsY(canvas, currentStepY);
        drawChart(chartValues.getOffset(), canvas, chartValues.getStep(), currentStepY);
        drawDates(chartValues.getOffset(), chartValues.getStep(), canvas);
    }
    private float calculateTransitionY(){
        if(currentMetricsValues == null){
            return 0;
        }
        if(transitionAlphaY == FULL_ALPHA || oldMetricValues == null){
            return currentMetricsValues.yStep;
        }
        return ChartHelper.calculateTransitionStep(transitionAlphaY, currentMetricsValues.yStep, oldMetricValues.yStep);
    }


    @Override
    protected void onSizeChanged(int widthNew, int heightNew, int oldw, int oldh) {
        super.onSizeChanged(widthNew, heightNew, oldw, oldh);
        baseLine = heightNew - verticalPadding;
        chartHeight = Math.max(baseLine - verticalPadding, 0);
        if(currentMetricsValues != null){
            currentMetricsValues = new MetricsValues(currentMetricsValues.maxValueY, chartHeight);
        }
    }
    private void requestInvalidation(){
        invalidateRequestsSubject.onNext(true);
    }

    private void drawChart(float startOffset, Canvas canvas, float stepX, float stepY) {
        int firstIndex = currentDiapason.getStartIndex();
        int lastIndex = currentDiapason.getEndIndex();
        int graphsSize = chart.getGraphsList().size();
        for(int index = 0; index < graphsSize; ++index){
            Chart.GraphData graph = chart.getGraphsList().get(index);
            List<Integer> selectedValues = graph.getValues().subList(firstIndex, lastIndex + 1);
            if(currentSelection.get(index)) {
                drawGraph(startOffset, graph.getColor(), FULL_ALPHA, canvas, selectedValues, stepX, stepY);
            }
        }
    }

    private void drawGraph(float startOffset, int color, int alpha, Canvas canvas, List<Integer> values, float stepX, float stepY){
        graphPaint.setColor(color);
        graphPaint.setAlpha(alpha);
        Path path = ChartHelper.buildGraphPath(values, baseLine, stepX, stepY);
        path.offset(-startOffset,0);
        canvas.drawPath(path,graphPaint);
    }

    private void drawDates(float startOffset, float stepX, Canvas canvas){
        metricTextPaint.setAlpha(FULL_ALPHA);
        int startIndex = currentDiapason.getStartIndex();
        int lastChartIndex = chart.getValuesX().size() - 1;
        int textPositionY = (canvas.getHeight() + baseLine) / 2;
        if(lastChartIndex == currentDiapason.getEndIndex()){
            long date = chart.getValuesX().get(lastChartIndex);
            drawAtTheEndOfChart(date, canvas, textPositionY);
        }
        if(datesTransitionAlpha == FULL_ALPHA || oldDatesStep == 0){
            drawJustCurrentDates(lastChartIndex, startIndex, stepX, startOffset, canvas, datesTransitionAlpha);
        } else {
            drawTransitionDates(lastChartIndex,startIndex,stepX,startOffset,canvas);
        }
    }
    private void drawJustCurrentDates(int lastChartIndex, int startIndex, float stepX, float startOffset, Canvas canvas, int alpha){
        metricTextPaint.setAlpha(FULL_ALPHA);
        int textPositionY = (canvas.getHeight() + baseLine) / 2;
        for(int index = lastChartIndex - currentDatesStep; index >= startIndex; index-=currentDatesStep){
            if(index > currentDiapason.getEndIndex()){
                continue;
            }
            long date = chart.getValuesX().get(index);
            drawInMiddleOf(stepX * (index - startIndex) - startOffset, date, canvas, textPositionY, alpha);
        }
    }
    private void drawTransitionDates(int lastChartIndex, int startIndex, float stepX, float startOffset, Canvas canvas){
        int textPositionY = (canvas.getHeight() + baseLine) / 2;
        int minStep = Math.min(currentDatesStep, oldDatesStep);
        for(int shift = minStep; shift < lastChartIndex; shift+=minStep){
            int index = lastChartIndex - shift;
            if(index > currentDiapason.getEndIndex()){
                continue;
            }
            if(index < currentDiapason.getStartIndex()){
                break;
            }
            long date = chart.getValuesX().get(index);
            if(ChartHelper.isShiftInStepsArray(shift, currentDatesStep) && ChartHelper.isShiftInStepsArray(shift,oldDatesStep)){
                drawInMiddleOf(stepX * (index - startIndex) - startOffset, date, canvas, textPositionY, FULL_ALPHA);
            } else if (ChartHelper.isShiftInStepsArray(shift, currentDatesStep)){
                drawInMiddleOf(stepX * (index - startIndex) - startOffset, date, canvas, textPositionY, datesTransitionAlpha);
            } else {
                drawInMiddleOf(stepX * (index - startIndex) - startOffset, date, canvas, textPositionY, FULL_ALPHA - datesTransitionAlpha);
            }
        }
    }

    private void drawInMiddleOf(float xCoordinate, long date, Canvas canvas, int textPositionY, int alpha){
        metricTextPaint.setAlpha(alpha);
        String text = DateHelper.getShortDayString(new Date(date));
        float width = metricTextPaint.measureText(text);
        float textStartX = xCoordinate - (width * 0.5f);
        canvas.drawText(text,textStartX,textPositionY,metricTextPaint);
    }
    private void drawAtTheEndOfChart(long date, Canvas canvas, int textPositionY){
        if(oldDatesStep == 0){
            metricTextPaint.setAlpha(datesTransitionAlpha);
        } else {
            metricTextPaint.setAlpha(FULL_ALPHA);
        }
        String text = DateHelper.getShortDayString(new Date(date));
        float width = metricTextPaint.measureText(text);
        canvas.drawText(text,canvas.getWidth() - width, textPositionY, metricTextPaint);
    }

    private void drawMetricsY(Canvas canvas, float currentStepY){
        drawMetricsY(canvas,currentStepY, transitionAlphaY);
        if(transitionAlphaY < FULL_ALPHA && oldMetricValues != null){
            float oldStepY = (float) chartHeight / oldMetricValues.maxValueY;
            drawMetricsY(canvas,oldStepY,FULL_ALPHA - transitionAlphaY);
        }
    }

    private void drawMetricsY(Canvas canvas, float stepY, int alpha) {
        metricPaint.setAlpha(alpha);
        metricTextPaint.setAlpha(alpha);
        float linePosition = baseLine;
        int width = canvas.getWidth();
        Path metricsPath = new Path();
        for(int index = 0; index <= METRICS_ITEMS_TO_DISPLAY; ++index){
            ChartHelper.addLineToPath(metricsPath,0,linePosition, width, linePosition);
            canvas.drawText(String.valueOf(index * currentMetricsValues.metricStep), 0, linePosition - metricTextPadding, metricTextPaint);
            linePosition -= currentMetricsValues.metricStep * stepY;
        }
        metricsPath.close();
        canvas.drawPath(metricsPath,metricPaint);
    }

    public void setChart(Chart newChart, List<Boolean> selectionList){
        CommonHelper.unsubscribeDisposable(selectedDiapasonDisposable);
        chart = newChart;
        oldSelection = null;
        currentSelection = selectionList;
        if(currentDiapason == null) {
            currentDiapason = new ChartDiapason(0, newChart.getValuesX().size() - 1, 0, 0, getWidth());
        }
        oldDatesStep = 0;
        currentDatesStep = ChartHelper.calculateStep(currentDiapason.getItemsInDiapason(),DATE_ITEMS_TO_DISPLAY);
        oldMetricValues = currentMetricsValues;
        currentMetricsValues = new MetricsValues(newChart.getMaxY(currentDiapason,currentSelection), chartHeight);
        requestInvalidation();
    }

    public void setNewSelection(List<Boolean> newSelectionList) {
        currentSelection = newSelectionList;
        oldSelection = currentSelection;
        requestInvalidation();
    }

    public void setPicker(DiapasonPicker picker){
        diapasonPicker = picker;
    }

    public void stopListeningDiapasonChanges(){
        CommonHelper.unsubscribeDisposable(selectedDiapasonDisposable);
    }

    public void subscribeToDiapasonChanges(){
        if(diapasonPicker == null){
            return;
        }
        CommonHelper.unsubscribeDisposable(selectedDiapasonDisposable);
        selectedDiapasonDisposable = diapasonPicker.getSelectedDiapasonObservable().subscribe(
                diapason -> {
                    updateDiapason(diapason);
                }, error -> {
                    error.printStackTrace();
                }
        );
    }
    private void updateDiapason(ChartDiapason newDiapason){
        oldDiapason = currentDiapason;
        currentDiapason = newDiapason;
        updateMetric();
        updateDatesSteps();
        requestInvalidation();
    }
    private void updateMetric(){
        int newMaxY = chart.getMaxY(currentDiapason, currentSelection);
        if(currentMetricsValues == null || currentMetricsValues.maxValueY != newMaxY){
            maxYSubject.onNext(newMaxY);
        }
    }
    private void updateDatesSteps(){
        if(oldDiapason == null || currentDiapason.getItemsInDiapason() != oldDiapason.getItemsInDiapason()){
            int newStep = ChartHelper.calculateStep(currentDiapason.getItemsInDiapason(), DATE_ITEMS_TO_DISPLAY);
            if(currentDatesStep != newStep){
                dateStepSubject.onNext(newStep);
            }
        }
    }

    private void launchMetricTransition() {
        CommonHelper.unsubscribeDisposable(yTransitionAnimationDisposable);
        transitionAlphaY = 0;
        yTransitionAnimationDisposable = (Observable.intervalRange(1L, Y_TRANSITION_ANIMATION_FRAME_COUNT,0L,25L, TimeUnit.MILLISECONDS,Schedulers.io())
                .map(lvl -> ChartHelper.calculateTransitionAlpha(lvl, Y_TRANSITION_ANIMATION_FRAME_COUNT))
                .subscribe(res -> {
                            transitionAlphaY = res;
                            Log.w(LOG_TAG,"TransitionAlpha:" +transitionAlphaY);
                            requestInvalidation();
                        }, error -> {
                            transitionAlphaY = FULL_ALPHA;
                            requestInvalidation();
                            error.printStackTrace();
                        }, () -> {
                            transitionAlphaY = FULL_ALPHA;
                        }
                ));
    }

    private void launchDatesTransition() {
        CommonHelper.unsubscribeDisposable(datesAnimationDisposable);
        datesTransitionAlpha = FULL_ALPHA;
        datesAnimationDisposable = (Observable.intervalRange(1L, DEFAULT_ANIMATION_FRAME_COUNT,0L,DEFAULT_ANIMATION_STEP, TimeUnit.MILLISECONDS,Schedulers.io())
                .map(lvl -> ChartHelper.calculateTransitionAlpha(lvl, DEFAULT_ANIMATION_FRAME_COUNT))
                .subscribe(res -> {
                            datesTransitionAlpha = res;
                            requestInvalidation();
                        }, error -> {
                            datesTransitionAlpha = FULL_ALPHA;
                            requestInvalidation();
                            error.printStackTrace();
                        }, () -> {
                            datesTransitionAlpha = FULL_ALPHA;
                        }
                ));
    }

    class MetricsValues{
        int maxValueY;
        int metricStep;
        float yStep;

        MetricsValues(int maxValueY, int chartHeight) {
            this.maxValueY = maxValueY;
            yStep = (float) chartHeight / maxValueY;
            metricStep = MathHelper.floorNumberToFirstToDigits(maxValueY) / METRICS_ITEMS_TO_DISPLAY;
        }
    }

    public interface DiapasonPicker{
        Observable<ChartDiapason> getSelectedDiapasonObservable();
    }
}

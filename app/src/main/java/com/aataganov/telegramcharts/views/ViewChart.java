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
import com.aataganov.telegramcharts.helpers.MathHelper;
import com.aataganov.telegramcharts.models.Chart;
import com.aataganov.telegramcharts.utils.ChartHelper;
import com.aataganov.telegramcharts.views.models.ChartDiapason;

import java.util.Collections;
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
    private static final int METRICS_ITEMS_TO_DISPLAY = 5;
    private static final String LOG_TAG = ViewChart.class.getSimpleName();

    ChartDiapason currentDiapason;
    ChartDiapason oldDiapason;

    private Chart chart;
    List<Boolean> oldSelection = Collections.emptyList();
    List<Boolean> currentSelection = Collections.emptyList();

    private Paint graphPaint = new Paint();
    private Paint metricPaint = new Paint();
    private Paint metricTextPaint = new Paint();

    int verticalPadding;
    int metricTextPadding;
    int baseLine = 0;
    int chartHeight = 0;

    int transitionAlphaY = FULL_ALPHA;

    MetricsValues currentMetricsValues = new MetricsValues(0);
    MetricsValues oldMetricValues;

    private CompositeDisposable viewBag = new CompositeDisposable();
    private Disposable transitionAnimationDisposable;
    private Disposable yTransitionAnimationDisposable;
    private DiapasonPicker diapasonPicker;
    private Disposable selectedDiapasonDisposable;
    private PublishSubject<Boolean> invalidateRequestsSubject = PublishSubject.create();
    private PublishSubject<Integer> maxYSubject = PublishSubject.create();

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
                        currentMetricsValues = new MetricsValues(newMaxY);
                        launchMetricTransition();
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
        long currtime = System.currentTimeMillis();
        canvas.drawColor(Color.WHITE);
        if(chart == null) {
            return;
        }
        float currentStepY = (float) chartHeight / currentMetricsValues.maxValueY;
        float currentStepX = (float) getWidth() / currentDiapason.getItemsInDiapason();
        drawMetricsY(canvas, currentStepY);
        drawChart(canvas, currentStepX, currentStepY);
        Log.w(LOG_TAG,"DRAW TIME:" + (System.currentTimeMillis() - currtime));
    }

    @Override
    protected void onSizeChanged(int widthNew, int heightNew, int oldw, int oldh) {
        super.onSizeChanged(widthNew, heightNew, oldw, oldh);
        baseLine = heightNew - verticalPadding;
        chartHeight = Math.max(baseLine - verticalPadding, 0);
    }
    private void requestInvalidation(){
        invalidateRequestsSubject.onNext(true);
    }

    private void drawChart(Canvas canvas, float stepX, float stepY) {
        int firstIndex = currentDiapason.getStartIndex();
        int lastIndex = currentDiapason.getEndIndex();
        int graphsSize = chart.getGraphsList().size();
        for(int index = 0; index < graphsSize; ++index){
            Chart.GraphData graph = chart.getGraphsList().get(index);
            List<Integer> selectedValues = graph.getValues().subList(firstIndex, lastIndex);
            if(currentSelection.get(index)) {
                drawGraph(graph.getColor(), FULL_ALPHA, canvas, selectedValues, stepX, stepY);
            }
        }
    }

    private void drawGraph(int color, int alpha, Canvas canvas, List<Integer> values, float stepX, float stepY){
        graphPaint.setColor(color);
        graphPaint.setAlpha(alpha);
        Path path = ChartHelper.buildGraphPath(values, baseLine, stepX, stepY);
        canvas.drawPath(path,graphPaint);
    }

    private void drawMetricsY(Canvas canvas, float currentStepY){
        Log.w(LOG_TAG," DRAW METRICS:" + transitionAlphaY);
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
            currentDiapason = new ChartDiapason(0, newChart.getValuesX().size() - 1);
        }
        oldMetricValues = currentMetricsValues;
        currentMetricsValues = new MetricsValues(newChart.getMaxY(currentDiapason,currentSelection));
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
        requestInvalidation();
    }
    private void updateMetric(){
        int newMaxY = chart.getMaxY(currentDiapason, currentSelection);
        if(currentMetricsValues == null || currentMetricsValues.maxValueY != newMaxY){
            maxYSubject.onNext(newMaxY);
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


    class MetricsValues{
        int maxValueY;
        int metricStep = 0;

        public MetricsValues(int maxValueY) {
            this.maxValueY = maxValueY;
            metricStep = MathHelper.floorNumberToFirstToDigits(maxValueY) / METRICS_ITEMS_TO_DISPLAY;
        }
    }

    public interface DiapasonPicker{
        Observable<ChartDiapason> getSelectedDiapasonObservable();
    }
}

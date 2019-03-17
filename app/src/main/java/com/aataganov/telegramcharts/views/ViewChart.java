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

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.aataganov.telegramcharts.helpers.Constants.FULL_ALPHA;

public class ViewChart extends View {
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

    MetricsValues metricsValues = new MetricsValues();

    private CompositeDisposable viewBag = new CompositeDisposable();
    private Disposable transitionAnimationDisposable;

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
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        CommonHelper.unsubscribeDisposeBag(viewBag);
        CommonHelper.unsubscribeDisposable(transitionAnimationDisposable);
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
        float stepY = (float) chartHeight / metricsValues.maxValueY;
        float stepX = (float) getWidth() / currentDiapason.getItemsInDiapason();
        drawMetricsY(canvas, stepY);
        drawChart(canvas, stepX, stepY);
    }

    @Override
    protected void onSizeChanged(int widthNew, int heightNew, int oldw, int oldh) {
        super.onSizeChanged(widthNew, heightNew, oldw, oldh);
        baseLine = heightNew - verticalPadding;
        chartHeight = Math.max(baseLine - verticalPadding, 0);
    }

    private void drawChart(Canvas canvas, float stepX, float stepY) {
        int graphsSize = chart.getGraphsList().size();
        for(int index = 0; index < graphsSize; ++index){
            Chart.GraphData graph = chart.getGraphsList().get(index);
            if(currentSelection.get(index)) {
                drawGraph(graph.getColor(), FULL_ALPHA, canvas, graph.getValues(), stepX, stepY);
            }
        }
    }

    private void drawGraph(int color, int alpha, Canvas canvas, List<Integer> values, float stepX, float stepY){
        graphPaint.setColor(color);
        graphPaint.setAlpha(alpha);
        Path path = ChartHelper.buildGraphPath(values, baseLine, stepX, stepY);
        canvas.drawPath(path,graphPaint);
    }

    private void drawMetricsY(Canvas canvas, float stepY) {
        float linePosition = baseLine;
        int width = canvas.getWidth();
        Path metricsPath = new Path();
        for(int index = 0; index <= METRICS_ITEMS_TO_DISPLAY; ++index){
            Log.w(LOG_TAG,"CharMetric: LP: " + linePosition + "Index: " + index);
            ChartHelper.addLineToPath(metricsPath,0,linePosition, width, linePosition);
            canvas.drawText(String.valueOf(index * metricsValues.metricStep), 0, linePosition - metricTextPadding, metricTextPaint);
            linePosition -= metricsValues.metricStep * stepY;
        }
        metricsPath.close();
        canvas.drawPath(metricsPath,metricPaint);
    }

    public void setChart(Chart newChart, List<Boolean> selectionList){
        chart = newChart;
        metricsValues.update(newChart.getMaxY());
        oldSelection = null;
        currentSelection = selectionList;
        currentDiapason = new ChartDiapason(0, newChart.getValuesX().size() - 1);
        oldDiapason = null;
        postInvalidate();
    }

    public void setNewSelection(List<Boolean> newSelecitonList) {
        currentSelection = newSelecitonList;
        oldSelection = currentSelection;
        postInvalidate();
    }


    class MetricsValues{
        int maxValueY;
        int metricStep = 0;

        public void update(int newValue){
            maxValueY = newValue;
            metricStep = MathHelper.floorNumberToFirstToDigits(maxValueY) / METRICS_ITEMS_TO_DISPLAY;
        }
    }
}

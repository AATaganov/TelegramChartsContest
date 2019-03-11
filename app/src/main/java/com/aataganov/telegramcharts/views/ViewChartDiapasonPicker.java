package com.aataganov.telegramcharts.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.models.Chart;
import com.aataganov.telegramcharts.utils.ChartHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ViewChartDiapasonPicker extends View {
    private static final String LOG_TAG = ViewChartDiapasonPicker.class.getSimpleName();

    public ViewChartDiapasonPicker(Context context) {
        super(context);
        init();
    }

    private void init() {
        initPaints();
    }

    public ViewChartDiapasonPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ViewChartDiapasonPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
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
        viewBag = null;
    }

    private Chart chart;
    private Paint graphPaint = new Paint();
    private Paint textPaint = new Paint();
    private ViewValues viewValues = new ViewValues();
    private CompositeDisposable viewBag = new CompositeDisposable();

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateValues();
    }

    public void setChart(Chart chart) {
        this.chart = chart;
        updateValues();

    }
    private void updateValues(){
        viewBag.add(Single.fromCallable(this::recalculateValues)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    postInvalidate();
                }, error -> {
                    error.printStackTrace();
                    Log.w(LOG_TAG,"Couldn't update chart values");
                }));
    }

    private void initPaints(){
        graphPaint.setAntiAlias(true);
        graphPaint.setStrokeWidth(3);
        graphPaint.setStyle(Paint.Style.STROKE);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.LTGRAY);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.LTGRAY);
        if(chart == null){
            return;
        }
        drawChart(Color.GREEN,canvas,chart.getValuesY0());
        drawChart(Color.RED,canvas,chart.getValuesY1());
    }

    private void drawChart(int color, Canvas canvas, List<Integer> values){
        graphPaint.setColor(color);
        Path path = ChartHelper.drawChart(values, viewValues.maxY, viewValues.xStep, viewValues.yStep);
        canvas.drawPath(path,graphPaint);
    }

    private boolean recalculateValues(){
        viewValues.update(chart,this);
        return true;
    }


    class ViewValues{
        float xStep = 0;
        float yStep = 0;
        float maxY = 0;
        public void update(Chart chart, View view){
            if(chart == null){
                return;
            }
            xStep = ((float) view.getWidth()) / chart.getValuesX().size();
            int maxY0 = Collections.max(chart.getValuesY0());
            int maxY1 = Collections.max(chart.getValuesY1());
            maxY = Math.max(maxY0,maxY1);
            yStep = ((float) view.getHeight()) / maxY;
        }
    }

}

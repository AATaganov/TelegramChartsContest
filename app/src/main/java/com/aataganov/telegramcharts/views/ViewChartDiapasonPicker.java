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
import com.aataganov.telegramcharts.models.Chart;
import com.aataganov.telegramcharts.utils.ChartHelper;
import com.aataganov.telegramcharts.views.models.SelectedDiapason;
import com.aataganov.telegramcharts.views.models.StepValues;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ViewChartDiapasonPicker extends View {
    private static final String LOG_TAG = ViewChartDiapasonPicker.class.getSimpleName();
    public static final int HALF_ALPHA = 128;

    public ViewChartDiapasonPicker(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        selectedDiapason = new SelectedDiapason(context.getResources().getDimensionPixelSize(R.dimen.diapason_selection_edge_width));
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
    private Paint diapasonEdgesPaint = new Paint();
    private Paint diapasonSkipPaint = new Paint();
    private StepValues stepValues = new StepValues();
    private SelectedDiapason selectedDiapason;
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
        diapasonEdgesPaint.setStyle(Paint.Style.FILL);
        diapasonEdgesPaint.setColor(Color.DKGRAY);
        diapasonEdgesPaint.setAlpha(HALF_ALPHA);
        diapasonSkipPaint.setStyle(Paint.Style.FILL);
        diapasonSkipPaint.setColor(Color.LTGRAY);
        diapasonSkipPaint.setAlpha(HALF_ALPHA);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.LTGRAY);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        if(chart == null){
            return;
        }
        drawChart(Color.GREEN,canvas,chart.getValuesY0());
        drawChart(Color.RED,canvas,chart.getValuesY1());
        drawDiapasonSkipAreas(canvas);
        drawDiapasonEdges(canvas);
    }

    private void drawChart(int color, Canvas canvas, List<Integer> values){
        graphPaint.setColor(color);
        Path path = ChartHelper.drawChart(values, stepValues.getMaxY(), stepValues.getxStep(), stepValues.getyStep());
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
        if(selectedDiapason.needToDrawStartSkip()){
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
}

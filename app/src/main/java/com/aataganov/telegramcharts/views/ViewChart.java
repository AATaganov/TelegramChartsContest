package com.aataganov.telegramcharts.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.aataganov.telegramcharts.R;
import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.helpers.DateHelper;
import com.aataganov.telegramcharts.helpers.ListHelper;
import com.aataganov.telegramcharts.helpers.MathHelper;
import com.aataganov.telegramcharts.models.Chart;
import com.aataganov.telegramcharts.utils.ChartHelper;
import com.aataganov.telegramcharts.views.models.ChartDiapason;

import java.util.ArrayList;
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
    private Paint backgroundPaint = new Paint();

    private Paint selectionCardDatePaint = new Paint();
    private Paint selectionCardValuePaint = new Paint();
    private Paint selectionCardNamePaint = new Paint();
    private Paint selectionCardBackgroundPaint = new Paint();

    int verticalPadding;
    int metricTextPadding;
    int stokeWidth;
    int selectedPointRadius;
    int baseLine = 0;
    int datesTextBaseline = 0;
    int chartHeight = 0;

    int chartSelectionCardPadding;
    int chartSelectionCardDateTextSize;
    int chartSelectionCardNameTextSize;
    int chartSelectionCardValuesTextSize;

    int currentSelectedIndex = ListHelper.EMPTY_ID;

    MetricsValues currentMetricsValues;
    MetricsValues oldMetricValues;

    private CompositeDisposable viewBag = new CompositeDisposable();

    private AnimationTimer metricTransitionAnimation = new AnimationTimer(DEFAULT_ANIMATION_FRAME_COUNT,DEFAULT_ANIMATION_STEP);
    private AnimationTimer datesAnimation = new AnimationTimer(DEFAULT_ANIMATION_FRAME_COUNT,DEFAULT_ANIMATION_STEP);
    private AnimationTimer selectionAnimation = new AnimationTimer(DEFAULT_ANIMATION_FRAME_COUNT,DEFAULT_ANIMATION_STEP);
    private DiapasonPicker diapasonPicker;
    private Disposable selectedDiapasonDisposable;
    private PublishSubject<Boolean> invalidateRequestsSubject = PublishSubject.create();
    private PublishSubject<Integer> maxYSubject = PublishSubject.create();
    private PublishSubject<Integer> dateStepSubject = PublishSubject.create();
    private boolean freezeDraw;

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
        stokeWidth = context.getResources().getDimensionPixelSize(R.dimen.chart_graph_width);
        selectedPointRadius = context.getResources().getDimensionPixelSize(R.dimen.chart_selection_circle_radius);

        chartSelectionCardPadding = context.getResources().getDimensionPixelSize(R.dimen.chart_selection_card_padding);
        chartSelectionCardDateTextSize = context.getResources().getDimensionPixelSize(R.dimen.chart_selection_card_date_text_size);
        chartSelectionCardNameTextSize = context.getResources().getDimensionPixelSize(R.dimen.chart_selection_card_name_text_size);
        chartSelectionCardValuesTextSize = context.getResources().getDimensionPixelSize(R.dimen.chart_selection_card_value_text_size);

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
        metricTransitionAnimation.unsubscribe();
        datesAnimation.unsubscribe();
        selectionAnimation.unsubscribe();
        viewBag = null;
    }


    private void initPaints(){
        graphPaint.setAntiAlias(true);
        graphPaint.setStrokeWidth(stokeWidth);
        graphPaint.setStyle(Paint.Style.STROKE);

        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);

        metricPaint.setAntiAlias(true);
        metricPaint.setColor(Color.LTGRAY);
        metricPaint.setStyle(Paint.Style.STROKE);
        metricPaint.setStrokeWidth(1);

        metricTextPaint.setTextSize(getContext().getResources().getDimensionPixelSize(R.dimen.chart_metric_text_size));
        metricTextPaint.setAntiAlias(true);
        metricTextPaint.setColor(Color.GRAY);

//        selectionCardDatePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
//        selectionCardValuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        selectionCardDatePaint.setTextSize(chartSelectionCardDateTextSize);
        selectionCardNamePaint.setTextSize(chartSelectionCardNameTextSize);
        selectionCardValuePaint.setTextSize(chartSelectionCardValuesTextSize);

        selectionCardDatePaint.setColor(Color.BLACK);

        selectionCardDatePaint.setAntiAlias(true);
        selectionCardNamePaint.setAntiAlias(true);
        selectionCardValuePaint.setAntiAlias(true);


        selectionCardBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionCardBackgroundPaint.setColor(Color.WHITE);
        selectionCardBackgroundPaint.setShadowLayer(0.5f * stokeWidth , 0, 0, Color.GRAY);
        selectionCardBackgroundPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        if(chart == null || freezeDraw) {
            return;
        }
        ChartDiapason.DrawChartValues chartValues = currentDiapason.getDrawChartValues(getWidth());
        float currentStepY = calculateTransitionY();
        Log.w(LOG_TAG,"Current step:" + currentStepY + "currentDiapason: " + currentDiapason.getEndIndex() + " chartValues: " + chartValues.getStep());
        drawMetricsY(canvas, currentStepY);
        drawChart(chartValues.getOffset(), canvas, chartValues.getStep(), currentStepY);
        drawDates(chartValues.getOffset(), chartValues.getStep(), canvas);
        drawSelectedDate(chartValues, canvas, currentStepY);
    }

    private void drawSelectedDate(ChartDiapason.DrawChartValues chartValues, Canvas canvas, float stepY) {
        if(currentSelectedIndex < 0 || !MathHelper.isInRange(currentSelectedIndex, currentDiapason.getStartIndex(), currentDiapason.getEndIndex())){
            return;
        }
        float xCoordinate = (currentSelectedIndex - currentDiapason.getStartIndex()) * chartValues.getStep() - chartValues.getOffset();
        drawSelectionLine(xCoordinate, canvas);
        List<SelectedDateGraphData> selectionDataList = buildSelectionList(currentSelectedIndex);
        drawSelectionCircles(xCoordinate, canvas, stepY, selectionDataList);
        drawSelectedDateCard(canvas, xCoordinate, selectionDataList, chart.getValuesX().get(currentSelectedIndex));

    }

    private void drawSelectedDateCard(Canvas canvas, float xCoordinate, List<SelectedDateGraphData> selectionDataList, long date){
        String dateString = DateHelper.getDayOfMonthString(new Date(date));
        int selectedValuesSize = selectionDataList.size();
        int valuesLines = (int) Math.ceil(selectedValuesSize * 0.5f);
        int lineSize = chartSelectionCardPadding + chartSelectionCardValuesTextSize + chartSelectionCardNameTextSize;
        float leftSideWidth = 0;
        float rightSideWidth = 0;
        for(int index = 0; index < selectedValuesSize; ++index){
            SelectedDateGraphData item = selectionDataList.get(index);
            if(index % 2 == 0){
                leftSideWidth = Math.max(leftSideWidth, selectionCardValuePaint.measureText(String.valueOf(item.value)));
                leftSideWidth = Math.max(leftSideWidth, selectionCardNamePaint.measureText(String.valueOf(item.name)));
            } else {
                rightSideWidth = Math.max(rightSideWidth, selectionCardValuePaint.measureText(String.valueOf(item.value)));
                rightSideWidth = Math.max(rightSideWidth, selectionCardNamePaint.measureText(String.valueOf(item.name)));
            }
        }
        int doublePadding = chartSelectionCardPadding * 2;
        float cardWidth = Math.max(selectionCardDatePaint.measureText(dateString), leftSideWidth + doublePadding + rightSideWidth);
        cardWidth += doublePadding;
        float cardHeight = doublePadding + (valuesLines * lineSize) + chartSelectionCardDateTextSize;

        float rectStart = xCoordinate - (0.2f * cardWidth);
        rectStart = Math.max(rectStart, chartSelectionCardPadding);
        rectStart = Math.min(rectStart, getWidth() - cardWidth - chartSelectionCardPadding);
        RectF cardRect = new RectF(rectStart,chartSelectionCardPadding,rectStart + cardWidth, chartSelectionCardPadding + cardHeight);

        canvas.drawRoundRect(cardRect,chartSelectionCardPadding * 0.5f, chartSelectionCardPadding * 0.5f, selectionCardBackgroundPaint);

        float currentBaseline = cardRect.top + chartSelectionCardDateTextSize + chartSelectionCardPadding;
        float textLeftBaseline = cardRect.left + chartSelectionCardPadding;
        canvas.drawText(dateString,textLeftBaseline,currentBaseline, selectionCardDatePaint);

        for(int index = 0; index < selectedValuesSize; ++index){
            SelectedDateGraphData item = selectionDataList.get(index);
            selectionCardValuePaint.setColor(item.color);
            selectionCardValuePaint.setAlpha(item.alpha);
            selectionCardNamePaint.setColor(item.color);
            selectionCardNamePaint.setAlpha(item.alpha);
            if(index % 2 == 0){
                currentBaseline += lineSize;
                canvas.drawText(String.valueOf(item.value),textLeftBaseline, currentBaseline - chartSelectionCardNameTextSize,selectionCardValuePaint);
                canvas.drawText(String.valueOf(item.name),textLeftBaseline, currentBaseline,selectionCardNamePaint);
            } else {
                canvas.drawText(String.valueOf(item.value),textLeftBaseline + leftSideWidth + doublePadding, currentBaseline - chartSelectionCardNameTextSize,selectionCardValuePaint);
                canvas.drawText(String.valueOf(item.name),textLeftBaseline + leftSideWidth + doublePadding, currentBaseline,selectionCardNamePaint);
            }
        }

    }

    private void drawSelectionLine(float xCoordinate,Canvas canvas){
        metricPaint.setAlpha(FULL_ALPHA);
        canvas.drawLine(xCoordinate,verticalPadding,xCoordinate,baseLine,metricPaint);
    }
    private List<SelectedDateGraphData> buildSelectionList(int selectedIndex){
        List<SelectedDateGraphData> selectionDataList = new ArrayList<>();
        int graphsSize = chart.getGraphsList().size();
        int fadingAlpha = FULL_ALPHA - selectionAnimation.alpha;
        boolean hasOldSelection = ListHelper.hasItems(oldSelection);
        for(int index = 0; index < graphsSize; ++index) {
            Chart.GraphData graph = chart.getGraphsList().get(index);
            if(currentSelection.get(index)) {
                if(hasOldSelection && oldSelection.get(index)){
                    selectionDataList.add(new SelectedDateGraphData(graph,FULL_ALPHA, graph.getValues().get(selectedIndex)));
                } else if (selectionAnimation.alpha > 0){
                    selectionDataList.add(new SelectedDateGraphData(graph,selectionAnimation.alpha, graph.getValues().get(selectedIndex)));
                }
            } else if(hasOldSelection && oldSelection.get(index) && fadingAlpha > 0){
                selectionDataList.add(new SelectedDateGraphData(graph, fadingAlpha, graph.getValues().get(selectedIndex)));
            }
        }
        return selectionDataList;
    }

    private void drawSelectionCircles(float xCoordinate, Canvas canvas, float stepY, List<SelectedDateGraphData> selectionDataList){
        for (SelectedDateGraphData data: selectionDataList){
            drawSelectionCircle(canvas, data.getColor(), data.getAlpha(), xCoordinate, stepY * data.getValue());
        }
    }
    class SelectedDateGraphData{
        int color;
        String name;
        int alpha;
        int value;

        public SelectedDateGraphData(Chart.GraphData graph, int alpha, int value) {
            this.color = graph.getColor();
            this.name = graph.getName();
            this.alpha = alpha;
            this.value = value;
        }

        public int getColor() {
            return color;
        }

        public String getName() {
            return name;
        }

        public int getAlpha() {
            return alpha;
        }

        public int getValue() {
            return value;
        }
    }

    private float calculateTransitionY(){
        float currentStep = (currentMetricsValues == null) ? 0 : currentMetricsValues.yStep;
        float oldStep = (oldMetricValues == null) ? 0 : oldMetricValues.yStep;
        return ChartHelper.calculateTransitionStep(metricTransitionAnimation.alpha, currentStep, oldStep);
    }


    @Override
    protected void onSizeChanged(int widthNew, int heightNew, int oldw, int oldh) {
        super.onSizeChanged(widthNew, heightNew, oldw, oldh);
        baseLine = heightNew - verticalPadding;
        chartHeight = Math.max(baseLine - verticalPadding, 0);
        if(currentMetricsValues != null){
            currentMetricsValues = new MetricsValues(currentMetricsValues.maxValueY, chartHeight);
        }
        datesTextBaseline = (int) (heightNew - ((float) verticalPadding * 0.2f));
    }
    private void requestInvalidation(){
        invalidateRequestsSubject.onNext(true);
    }

    private void drawChart(float startOffset, Canvas canvas, float stepX, float stepY) {
        int firstIndex = currentDiapason.getStartIndex();
        int lastIndex = currentDiapason.getEndIndex();
        int graphsSize = chart.getGraphsList().size();
        boolean hasOldSelection = ListHelper.hasItems(oldSelection);
        int fadingAlpha = FULL_ALPHA - selectionAnimation.alpha;
        for(int index = 0; index < graphsSize; ++index){
            Chart.GraphData graph = chart.getGraphsList().get(index);
            Log.w(LOG_TAG," SUBLIST OF values: " + graph.getValues().size() + " firstIndex: " + firstIndex + " last:" + (lastIndex + 1));
            List<Integer> selectedValues = graph.getValues().subList(firstIndex, Math.min(lastIndex + 1, graph.getValues().size()));
            if(currentSelection.get(index)) {
                if(hasOldSelection && oldSelection.get(index)){
                    drawGraph(startOffset, graph.getColor(), FULL_ALPHA, canvas, selectedValues, stepX, stepY);
                } else {
                    drawGraph(startOffset, graph.getColor(), selectionAnimation.alpha, canvas, selectedValues, stepX, stepY);
                }
            } else if(hasOldSelection && oldSelection.get(index)){
                drawGraph(startOffset, graph.getColor(), fadingAlpha, canvas, selectedValues, stepX, stepY);
            }
        }
    }

    private void drawSelectionCircle(Canvas canvas, int color, int alpha, float xCoordinate, float yCoordinate){
        graphPaint.setColor(color);
        graphPaint.setAlpha(alpha);
        canvas.drawCircle(xCoordinate,baseLine - yCoordinate,selectedPointRadius, backgroundPaint);
        canvas.drawCircle(xCoordinate,baseLine - yCoordinate,selectedPointRadius, graphPaint);
    }
    private void drawGraph(float startOffset, int color, int alpha, Canvas canvas, List<Integer> values, float stepX, float stepY){
        if(alpha == 0){
            return;
        }
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
        if(lastChartIndex == currentDiapason.getEndIndex()){
            long date = chart.getValuesX().get(lastChartIndex);
            drawAtTheEndOfChart(date, canvas, datesTextBaseline);
        }
        if(datesAnimation.alpha == FULL_ALPHA || oldDatesStep == 0){
            drawJustCurrentDates(lastChartIndex, startIndex, stepX, startOffset, canvas, datesAnimation.alpha);
        } else {
            drawTransitionDates(lastChartIndex,startIndex,stepX,startOffset,canvas);
        }
    }
    private void drawJustCurrentDates(int lastChartIndex, int startIndex, float stepX, float startOffset, Canvas canvas, int alpha){
        metricTextPaint.setAlpha(FULL_ALPHA);
        for(int index = lastChartIndex - currentDatesStep; index >= startIndex; index-=currentDatesStep){
            if(index > currentDiapason.getEndIndex()){
                continue;
            }
            long date = chart.getValuesX().get(index);
            drawInMiddleOf(stepX * (index - startIndex) - startOffset, date, canvas, datesTextBaseline, alpha);
        }
    }

    private void drawTransitionDates(int lastChartIndex, int startIndex, float stepX, float startOffset, Canvas canvas){
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
                drawInMiddleOf(stepX * (index - startIndex) - startOffset, date, canvas, datesTextBaseline, FULL_ALPHA);
            } else if (ChartHelper.isShiftInStepsArray(shift, currentDatesStep)){
                drawInMiddleOf(stepX * (index - startIndex) - startOffset, date, canvas, datesTextBaseline, datesAnimation.alpha);
            } else {
                drawInMiddleOf(stepX * (index - startIndex) - startOffset, date, canvas, datesTextBaseline, FULL_ALPHA - datesAnimation.alpha);
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
            metricTextPaint.setAlpha(datesAnimation.alpha);
        } else {
            metricTextPaint.setAlpha(FULL_ALPHA);
        }
        String text = DateHelper.getShortDayString(new Date(date));
        float width = metricTextPaint.measureText(text);
        canvas.drawText(text,canvas.getWidth() - width, textPositionY, metricTextPaint);
    }

    private void drawMetricsY(Canvas canvas, float currentStepY){
        drawMetricsY(canvas,currentStepY, metricTransitionAnimation.alpha);
        if(metricTransitionAnimation.alpha < FULL_ALPHA && oldMetricValues != null){
            float oldStepY = (float) chartHeight / oldMetricValues.maxValueY;
            drawMetricsY(canvas,oldStepY,FULL_ALPHA - metricTransitionAnimation.alpha);
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
        freezeDraw = true;
        CommonHelper.unsubscribeDisposable(selectedDiapasonDisposable);
        chart = newChart;
        oldSelection = ChartHelper.buildUnselectedList(selectionList.size());
        currentSelection = selectionList;
        currentSelectedIndex = ListHelper.EMPTY_ID;
        oldDiapason = null;
        currentDiapason = new ChartDiapason(0, newChart.getValuesX().size() - 1, 0, 0, getWidth(), newChart.getValuesX().size());
        oldDatesStep = 0;
        currentDatesStep = ChartHelper.calculateStep(currentDiapason.getItemsInDiapason(),DATE_ITEMS_TO_DISPLAY);
        oldMetricValues = currentMetricsValues;
        currentMetricsValues = new MetricsValues(newChart.getMaxY(currentDiapason,currentSelection), chartHeight);
        launchMetricTransition();
        launshSelectionTransition();
        launchDatesTransition();
        freezeDraw = false;
    }

    public void setNewSelection(List<Boolean> newSelectionList) {
        oldSelection = currentSelection;
        currentSelection = newSelectionList;
        updateMetric();
        launshSelectionTransition();
        requestInvalidation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & 255){
            case MotionEvent.ACTION_DOWN:
                Log.w(LOG_TAG," ON DOWN");
                return true;
            case MotionEvent.ACTION_UP:
                Log.w(LOG_TAG," ON UP");
                return onChartClick(event);
        }
        return super.onTouchEvent(event);
    }

    private boolean onChartClick(MotionEvent event){
        if(currentDiapason == null){
            return false;
        }
        int newSelectionIndex = ChartHelper.calculateClickedIndex(event.getX(), getWidth(), currentDiapason);
        if(newSelectionIndex == currentSelectedIndex){
            currentSelectedIndex = ListHelper.EMPTY_ID;
        } else {
            currentSelectedIndex = newSelectionIndex;
        }
        requestInvalidation();
        return true;
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
        if(chart == null || chart.getValuesX().size() != newDiapason.getItemsCount()){
            return;
        }
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
        metricTransitionAnimation.launch();
    }

    private void launchDatesTransition() {
        datesAnimation.launch();
    }

    private void launshSelectionTransition(){
        selectionAnimation.launch();
    }

    public void clearChart() {
        if(chart != null){
            setNewSelection(ChartHelper.buildUnselectedList(chart.getGraphsList().size()));
        }
    }

    class MetricsValues{
        int maxValueY;
        int metricStep;
        float yStep;

        MetricsValues(int maxValueY, int chartHeight) {
            this.maxValueY = maxValueY;
            if(maxValueY == 0){
                yStep = 0;
                metricStep = 0;
            } else {
                yStep = (float) chartHeight / maxValueY;
                metricStep = MathHelper.floorNumberToFirstToDigits(maxValueY) / METRICS_ITEMS_TO_DISPLAY;
            }
        }
    }

    public interface DiapasonPicker{
        Observable<ChartDiapason> getSelectedDiapasonObservable();
    }

    class AnimationTimer{
        private int frameCount;
        private long animationPeriod;
        private int alpha = FULL_ALPHA;
        private Disposable animationDisposable;

        public AnimationTimer(int frameCount, long animationPeriod) {
            this.frameCount = frameCount;
            this.animationPeriod = animationPeriod;
        }
        public void unsubscribe(){
            CommonHelper.unsubscribeDisposable(animationDisposable);
        }
        public void launch(){
            CommonHelper.unsubscribeDisposable(animationDisposable);
            alpha = FULL_ALPHA;
            animationDisposable = (Observable.intervalRange(1L, frameCount,0L,animationPeriod, TimeUnit.MILLISECONDS,Schedulers.io())
                    .map(lvl -> ChartHelper.calculateTransitionAlpha(lvl, frameCount))
                    .subscribe(res -> {
                                alpha = res;
                                requestInvalidation();
                            }, error -> {
                                alpha = FULL_ALPHA;
                                requestInvalidation();
                                error.printStackTrace();
                            }, () -> {
                                alpha = FULL_ALPHA;
                            }
                    ));
        }
    }
}

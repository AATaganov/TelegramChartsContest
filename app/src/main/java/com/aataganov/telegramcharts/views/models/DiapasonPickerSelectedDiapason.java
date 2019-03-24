package com.aataganov.telegramcharts.views.models;

import android.graphics.RectF;
import android.view.View;

import com.aataganov.telegramcharts.views.ViewChartDiapasonPicker;

public class DiapasonPickerSelectedDiapason {
    private static final int MIN_DIAPASON_ITEMS_COUNT = 5;
    final int DIAPASON_EDGE_SELECTION_WIDTH;
    final int DIAPASON_EDGE_SELECTION_HEIGHT;
    final float edgeTouchAreaPadding;
    private int verticalPadding;
    private int horizontalPadding;
    float startCoordinate = -1;
    float endCoordinate = 0;
    float minDistance = 0;
    float itemWidth;
    int itemsCount;
    private int viewHeight;
    private int viewWidth;
    private int widthWithoutPadding;
    RectF startSkip = new RectF();
    RectF endSkip = new RectF();
    RectF startEdge = new RectF();
    RectF endEdge = new RectF();
    RectF topEdge = new RectF();
    RectF bottomEdge = new RectF();

    public RectF getStartSkip() {
        return startSkip;
    }

    public RectF getEndSkip() {
        return endSkip;
    }

    public RectF getStartEdge() {
        return startEdge;
    }

    public RectF getEndEdge() {
        return endEdge;
    }

    public RectF getTopEdge() {
        return topEdge;
    }

    public RectF getBottomEdge() {
        return bottomEdge;
    }

    public DiapasonPickerSelectedDiapason(int edgeWidth, int edgeHeight,
                                          int touchSensibility,
                                          int verticalPadding,
                                          int horizontalPadding) {
        this.DIAPASON_EDGE_SELECTION_WIDTH = edgeWidth;
        this.DIAPASON_EDGE_SELECTION_HEIGHT = edgeHeight;
        this.edgeTouchAreaPadding = touchSensibility;
        this.verticalPadding = verticalPadding;
        this.horizontalPadding = horizontalPadding;
    }

    public void update(StepValues stepValues, View view){
        minDistance = Math.max(stepValues.getStepX() * MIN_DIAPASON_ITEMS_COUNT, DIAPASON_EDGE_SELECTION_WIDTH * 2);
        updateReactsStaticCoordinates(view);
        itemWidth = ((float) widthWithoutPadding) / stepValues.itemsCount;
        itemsCount = stepValues.itemsCount;
        resetValues();
    }

    boolean resetValues(){
        boolean updateStartResult = updateStart(verticalPadding);
        boolean updateEndResult = updateEnd(viewWidth);
        return (updateStartResult || updateEndResult);
    }

    boolean updateStart(float newStart){
        float validNewValue = Math.min(newStart, endCoordinate - minDistance);
        if(validNewValue < horizontalPadding){
            validNewValue = horizontalPadding;
        }
        if(validNewValue != startCoordinate){
            startCoordinate = validNewValue;
            recalculateStartReacts();
            return true;
        }
        return false;
    }

    public boolean needToDrawStartSkip(){
        return startSkip.right > 0;
    }

    public boolean needToDrawEndSkip(){
        return endSkip.left < endSkip.right;
    }

    boolean updateEnd(float newEnd){
        float validNewValue = Math.max(newEnd, startCoordinate + minDistance);
        float maxRightPosition = endSkip.right;
        if(validNewValue > maxRightPosition){
            validNewValue = maxRightPosition;
        }
        if(validNewValue != endCoordinate){
            endCoordinate = validNewValue;
            recalculateEndReacts();
            return true;
        }
        return false;
    }

    private float getSelectedAreaStartPosition(){
        return startEdge.right;
    }
    private boolean updateSelectedArea(float newX) {
        float areaWidth = endCoordinate - startCoordinate;
        float currentPosition = getSelectedAreaStartPosition();
        float difference = newX - currentPosition;
        if(difference == 0){
            return false;
        } else if(difference > 0){
            if(updateEnd(endCoordinate + difference)){
                updateStart(endCoordinate - areaWidth);
                return true;
            }
        } else {
            if(updateStart(startCoordinate + difference)){
                updateEnd(startCoordinate + areaWidth);
                return true;
            }
        }
        return false;
    }

    private void updateReactsStaticCoordinates(View view){
        viewHeight = view.getHeight();
        viewWidth = view.getWidth();
        widthWithoutPadding = viewWidth - horizontalPadding - horizontalPadding;
        int bottomCoordinate = viewHeight - verticalPadding;
        startSkip.left = horizontalPadding;
        startEdge.top = verticalPadding;
        startSkip.top = verticalPadding;
        topEdge.top = verticalPadding;
        topEdge.bottom = verticalPadding + DIAPASON_EDGE_SELECTION_HEIGHT;
        endSkip.top = verticalPadding;
        endEdge.top = verticalPadding;
        startEdge.bottom = bottomCoordinate;
        startSkip.bottom = bottomCoordinate;
        endSkip.bottom = bottomCoordinate;
        endEdge.bottom = bottomCoordinate;
        bottomEdge.bottom = bottomCoordinate;
        bottomEdge.top = bottomCoordinate - DIAPASON_EDGE_SELECTION_HEIGHT;
        endSkip.right = viewWidth - horizontalPadding;
    }
    void recalculateStartReacts(){
        float startEdgeLeft = Math.max(horizontalPadding, startCoordinate);
        startSkip.right = startEdgeLeft;
        startEdge.left = startEdgeLeft;
        startEdge.right = startEdgeLeft + DIAPASON_EDGE_SELECTION_WIDTH;
        topEdge.left = startEdge.right;
        bottomEdge.left = topEdge.left;
    }
    void recalculateEndReacts(){
        float endEdgeRight = Math.min(endSkip.right, endCoordinate);
        endSkip.left = endEdgeRight;
        endEdge.right = endEdgeRight;
        endEdge.left = endEdgeRight - DIAPASON_EDGE_SELECTION_WIDTH;
        topEdge.right = endEdge.left;
        bottomEdge.right = topEdge.right;
    }

    public ViewChartDiapasonPicker.TouchedArea getTouchedArea(float x){
        float selectedAreaEdgePadding = 0;
        if(endEdge.left - startEdge.right > edgeTouchAreaPadding * 2){
            selectedAreaEdgePadding = Math.min( 0.15f * (endEdge.left - startEdge.right), edgeTouchAreaPadding);
        }
        if(x < startEdge.left - edgeTouchAreaPadding || x > endEdge.right + edgeTouchAreaPadding){
            return ViewChartDiapasonPicker.TouchedArea.NONE;
        } else if(x < startEdge.right + selectedAreaEdgePadding){
            return ViewChartDiapasonPicker.TouchedArea.START_EDGE;
        } else if(x <= endEdge.left - selectedAreaEdgePadding){
            return ViewChartDiapasonPicker.TouchedArea.SELECTED_AREA;
        } else {
            return ViewChartDiapasonPicker.TouchedArea.END_EDGE;
        }
    }
    private float getSelectedAreaCenter(){
        return ((endEdge.left + startEdge.right) * 0.5f);
    }
    public float getAreaPosition(ViewChartDiapasonPicker.TouchedArea touchedArea) {
        switch (touchedArea){
            case START_EDGE:
                return startCoordinate;
            case END_EDGE:
                return endCoordinate;
            case SELECTED_AREA:
                return getSelectedAreaStartPosition();
        }
        return 0;
    }
    public float getSelectedAreaCenter(ViewChartDiapasonPicker.TouchedArea touchedArea){
        switch (touchedArea){
            case START_EDGE:
                return startEdge.centerX();
            case END_EDGE:
                return endEdge.centerX();
            case SELECTED_AREA:
                return getSelectedAreaCenter();
        }
        return 0;
    }

    public boolean moveToNewPosition(ViewChartDiapasonPicker.TouchedArea touchedArea, float newX) {
        switch (touchedArea){
            case START_EDGE:
                return updateStart(newX);
            case END_EDGE:
                return updateEnd(newX);
            case SELECTED_AREA:
                return updateSelectedArea(newX);
        }
        return false;
    }

    public ChartDiapason calculateDiapason(){
        int startIndex = 0;
        int endIndex;
        float startShift = 0f;
        float endShift = 0f;
        if(startCoordinate > horizontalPadding){
            startIndex = (int) (Math.floor((startCoordinate - horizontalPadding) / itemWidth));
            startShift = startCoordinate - horizontalPadding - (startIndex * itemWidth);
        }
        if(endCoordinate - horizontalPadding < widthWithoutPadding){
            endIndex = Math.min(itemsCount - 1,(int) (Math.ceil((endCoordinate - horizontalPadding) / itemWidth)));
            endShift = (endIndex * itemWidth) - (endCoordinate - horizontalPadding);
        } else {
            endIndex = itemsCount - 1;
        }
        return new ChartDiapason(startIndex, endIndex, startShift, endShift, endCoordinate - startCoordinate, itemsCount);

    }
}

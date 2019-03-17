package com.aataganov.telegramcharts.views.models;

import android.graphics.RectF;
import android.view.View;

import com.aataganov.telegramcharts.views.ViewChartDiapasonPicker;

public class DiapasonPickerSelectedDiapason {
    private static final int MIN_DIAPASON_ITEMS_COUNT = 5;
    final int DIAPASON_EDGE_SELECTION_WIDTH;
    final float edgeTouchAreaPadding;
    private int verticalPadding;
    private int horizontalPadding;
    float startCoordinate = -1;
    float endCoordinate = 0;
    float minDistance = 0;
    private int viewHeight;
    private int viewWidth;
    private int widthWithoutPadding;
    RectF startSkip = new RectF();
    RectF endSkip = new RectF();
    RectF startEdge = new RectF();
    RectF endEdge = new RectF();

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

    public DiapasonPickerSelectedDiapason(int edgeWidth, int verticalPadding, int horizontalPadding) {
        this.DIAPASON_EDGE_SELECTION_WIDTH = edgeWidth;
        this.edgeTouchAreaPadding = edgeWidth * 0.8f;
        this.verticalPadding = verticalPadding;
        this.horizontalPadding = horizontalPadding;
    }

    public void update(StepValues stepValues, View view){
        minDistance = Math.max(stepValues.getStepX() * MIN_DIAPASON_ITEMS_COUNT, DIAPASON_EDGE_SELECTION_WIDTH * 2);
        updateReactsStaticCoordinates(view);
        resetValues();
    }

    boolean resetValues(){
        return (updateStart(verticalPadding) || updateEnd(viewWidth));
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
        endSkip.top = verticalPadding;
        endEdge.top = verticalPadding;
        startEdge.bottom = bottomCoordinate;
        startSkip.bottom = bottomCoordinate;
        endSkip.bottom = bottomCoordinate;
        endEdge.bottom = bottomCoordinate;
        endSkip.right = viewWidth - horizontalPadding;
    }
    void recalculateStartReacts(){
        float startEdgeLeft = Math.max(horizontalPadding, startCoordinate);
        startSkip.right = startEdgeLeft;
        startEdge.left = startEdgeLeft;
        startEdge.right = startEdgeLeft + DIAPASON_EDGE_SELECTION_WIDTH;
    }
    void recalculateEndReacts(){
        float endEdgeRight = Math.min(endSkip.right, endCoordinate);
        endSkip.left = endEdgeRight;
        endEdge.right = endEdgeRight;
        endEdge.left = endEdgeRight - DIAPASON_EDGE_SELECTION_WIDTH;
    }

    public ViewChartDiapasonPicker.TouchedArea getTouchedArea(float x){
        if(x < startEdge.left - edgeTouchAreaPadding || x > endEdge.right + edgeTouchAreaPadding){
            return ViewChartDiapasonPicker.TouchedArea.NONE;
        } else if(x < startEdge.right + edgeTouchAreaPadding){
            return ViewChartDiapasonPicker.TouchedArea.START_EDGE;
        } else if(x <= endEdge.left - edgeTouchAreaPadding){
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

    public ChartDiapason calculateDiapason(int itemsCount){
        int startIndex = 0;
        int endIndex = 0;
        if(startCoordinate > horizontalPadding){
            startIndex = (int) (((startCoordinate - horizontalPadding) * itemsCount) / widthWithoutPadding);
        }
        if(endCoordinate - horizontalPadding < widthWithoutPadding){
            endIndex = (int) (((endCoordinate - horizontalPadding) * itemsCount) / widthWithoutPadding);
        } else {
            endIndex = itemsCount - 1;
        }
        return new ChartDiapason(startIndex, endIndex);

    }
}

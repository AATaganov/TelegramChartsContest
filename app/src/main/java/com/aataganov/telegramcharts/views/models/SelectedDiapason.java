package com.aataganov.telegramcharts.views.models;

import android.graphics.RectF;
import android.view.View;

import com.aataganov.telegramcharts.views.ViewChartDiapasonPicker;

public class SelectedDiapason {
    private static final int MIN_DIAPASON_ITEMS_COUNT = 5;
    final int DIAPASON_EDGE_SELECTION_WIDTH;
    final int edgeTouchArea;
    float startCoordinate = -1;
    float endCoordinate = 0;
    float minDistance = 0;
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

    public SelectedDiapason(int edgeWidth) {
        this.DIAPASON_EDGE_SELECTION_WIDTH = edgeWidth;
        this.edgeTouchArea = edgeWidth / 2;
    }

    public void update(StepValues stepValues, View view){
        minDistance = Math.max(stepValues.xStep * MIN_DIAPASON_ITEMS_COUNT, DIAPASON_EDGE_SELECTION_WIDTH * 2);
        updateRectsStaticCoordinates(view);
        resetValues(view);
    }

    boolean resetValues(View view){
        return (updateStart(0,view) || updateEnd(view.getWidth(),view));
    }

    boolean updateStart(float newStart, View view){
        float validNewValue = Math.min(newStart, endCoordinate - minDistance);
        if(validNewValue < 0){
            validNewValue = 0;
        }
        if(validNewValue != startCoordinate){
            startCoordinate = validNewValue;
            recalculateStartReacts(view);
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

    boolean updateEnd(float newEnd, View view){
        float validNewValue = Math.max(newEnd, startCoordinate + minDistance);
        int width = view.getWidth();
        if(validNewValue > width){
            validNewValue = width;
        }
        if(validNewValue != endCoordinate){
            endCoordinate = validNewValue;
            recalculateEndReacts(view);
            return true;
        }
        return false;
    }

    private float getSelectedAreaStartPosition(){
        return startEdge.right;
    }
    private boolean updateSelectedArea(float newX, View view) {
        float areaWidth = endCoordinate - startCoordinate;
        float currentPosition = getSelectedAreaStartPosition();
        float difference = newX - currentPosition;
        if(difference == 0){
            return false;
        } else if(difference > 0){
            if(updateEnd(endCoordinate + difference, view)){
                updateStart(endCoordinate - areaWidth,view);
                return true;
            }
        } else {
            if(updateStart(startCoordinate + difference, view)){
                updateEnd(startCoordinate + areaWidth,view);
                return true;
            }
        }
        return false;
    }

    private void updateRectsStaticCoordinates(View view){
        int height = view.getHeight();
        startEdge.bottom = height;
        startSkip.bottom = height;
        endSkip.bottom = height;
        endEdge.bottom = height;
        endSkip.right = view.getWidth();
    }
    void recalculateStartReacts(View view){
        float startEdgeLeft = Math.max(0, startCoordinate);
        startSkip.right = startEdgeLeft;
        startEdge.left = startEdgeLeft;
        startEdge.right = startEdgeLeft + DIAPASON_EDGE_SELECTION_WIDTH;
    }
    void recalculateEndReacts(View view){
        float endEdgeRight = Math.min(view.getWidth(), endCoordinate);
        endSkip.left = endEdgeRight;
        endEdge.right = endEdgeRight;
        endEdge.left = endEdgeRight - DIAPASON_EDGE_SELECTION_WIDTH;
    }

    public ViewChartDiapasonPicker.TouchedArea getTouchedArea(float x){
        if(x < startEdge.left - edgeTouchArea || x > endEdge.right + edgeTouchArea){
            return ViewChartDiapasonPicker.TouchedArea.NONE;
        } else if(x < startEdge.right + edgeTouchArea){
            return ViewChartDiapasonPicker.TouchedArea.START_EDGE;
        } else if(x <= endEdge.left - edgeTouchArea){
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
                return startEdge.left;
            case END_EDGE:
                return endEdge.left;
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

    public boolean moveToNewPosition(ViewChartDiapasonPicker.TouchedArea touchedArea, float newX, View view) {
        switch (touchedArea){
            case START_EDGE:
                return updateStart(newX,view);
            case END_EDGE:
                return updateEnd(newX,view);
            case SELECTED_AREA:
                return updateSelectedArea(newX,view);
        }
        return false;
    }
}

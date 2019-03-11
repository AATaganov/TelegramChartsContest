package com.aataganov.telegramcharts.views.models;

import android.graphics.RectF;
import android.view.View;

public class SelectedDiapason {
    private static final int MIN_DIAPASON_ITEMS_COUNT = 5;
    final int DIAPASON_EDGE_SELECTION_WIDTH;
    float startCoordinate = 0;
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
    }

    public void update(StepValues stepValues, View view){
        minDistance = Math.max(stepValues.xStep * MIN_DIAPASON_ITEMS_COUNT, DIAPASON_EDGE_SELECTION_WIDTH * 2);
        updateRectsStaticCoordinates(view);
        updateValues(0+200,view.getWidth() - 150,view);
    }

    boolean updateValues(float newStart, float newEnd, View view){
        boolean updated = (updateStart(newStart) || updateEnd(newEnd,view));
        if(updated){
            recalculateReacts(view);
        }
        return updated;
    }

    boolean updateStart(float newStart){
        float validNewValue = Math.min(newStart, endCoordinate - minDistance);
        if(validNewValue < 0){
            validNewValue = 0;
        }
        if(validNewValue != startCoordinate){
            startCoordinate = validNewValue;
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
            return true;
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
    void recalculateReacts(View view){
        float startEdgeLeft = Math.max(0, startCoordinate);
        float endEdgeRight = Math.min(view.getWidth(), endCoordinate);
        startSkip.right = startEdgeLeft;
        startEdge.left = startEdgeLeft;
        startEdge.right = startEdgeLeft + DIAPASON_EDGE_SELECTION_WIDTH;

        endSkip.left = endEdgeRight;
        endEdge.right = endEdgeRight;
        endEdge.left = endEdgeRight - DIAPASON_EDGE_SELECTION_WIDTH;
    }


}

package com.aataganov.telegramcharts.views.models;

import android.view.View;

import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.helpers.Constants;
import com.aataganov.telegramcharts.models.Chart;
import com.aataganov.telegramcharts.utils.ChartHelper;

import java.util.List;

public class StepValues {
    private float stepX = 0f;
    private float stepY = 0f;
    private float stepYOld = 0f;
    private float yCenter = 0f;
    int verticalPadding;
    int horizontalPadding;
    int itemsCount;
    private int heightWithoutPadding;

    public StepValues(int verticalPadding, int horizontalPadding) {
        this.verticalPadding = verticalPadding;
        this.horizontalPadding = horizontalPadding;
    }

    public void update(Chart chart, View view){
        if(chart == null){
            return;
        }
        itemsCount = chart.getValuesX().size();
        int widthWithoutPadding = CommonHelper.calculateSizeWithPadding(view.getWidth(), horizontalPadding);
        heightWithoutPadding = CommonHelper.calculateSizeWithPadding(view.getHeight(), verticalPadding);
        stepX = ((float) widthWithoutPadding) / itemsCount;
        long maxY = ChartHelper.calculateMaxY(chart);
        stepY = ((float) heightWithoutPadding) / maxY;
        stepYOld = stepY;
        yCenter = view.getHeight() * 0.5f;
    }

    public int getHeightWithoutPadding() {
        return heightWithoutPadding;
    }

    public float getStepX() {
        return stepX;
    }

    public float getyCenter() {
        return yCenter;
    }

    public void updateStepY(Chart chart, List<Boolean> selectionList){
        stepYOld = stepY;
        long newMaxY = ChartHelper.calculateMaxY(chart, selectionList);
        stepY = ((float) heightWithoutPadding) / newMaxY;
    }
    public float calculateTransitionStep(int transitionAlpha){
        return ChartHelper.calculateTransitionStep(transitionAlpha,stepY, stepYOld);
    }
}

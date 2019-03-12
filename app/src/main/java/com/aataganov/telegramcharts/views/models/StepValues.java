package com.aataganov.telegramcharts.views.models;

import android.view.View;

import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.models.Chart;

import java.util.Collections;

public class StepValues {
    private float xStep = 0f;
    private float yStep = 0f;
    private float maxY = 0f;
    private float yCenter = 0f;
    int verticalPadding;
    int horizontalPadding;

    public StepValues(int verticalPadding, int horizontalPadding) {
        this.verticalPadding = verticalPadding;
        this.horizontalPadding = horizontalPadding;
    }

    public void update(Chart chart, View view){
        if(chart == null){
            return;
        }
        int widthWithoutPadding = CommonHelper.calculateSizeWithPadding(view.getWidth(), horizontalPadding);
        int heightWithoutPadding = CommonHelper.calculateSizeWithPadding(view.getHeight(), verticalPadding);
        xStep = ((float) widthWithoutPadding) / chart.getValuesX().size();
        maxY = 0;
        for (Chart.GraphData line:
             chart.getLines()) {
            if(maxY < line.getMaxValue()){
                maxY = line.getMaxValue();
            }
        }
        yStep = ((float) heightWithoutPadding) / maxY;
        yCenter = view.getHeight() * 0.5f;
    }

    public float getxStep() {
        return xStep;
    }

    public float getyStep() {
        return yStep;
    }

    public float getMaxY() {
        return maxY;
    }

    public float getyCenter() {
        return yCenter;
    }
}

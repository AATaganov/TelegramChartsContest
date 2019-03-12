package com.aataganov.telegramcharts.views.models;

import android.view.View;

import com.aataganov.telegramcharts.models.Chart;

import java.util.Collections;

public class StepValues {
    private float xStep = 0f;
    private float yStep = 0f;
    private float maxY = 0f;
    private float yCenter = 0f;
    public void update(Chart chart, View view){
        if(chart == null){
            return;
        }
        xStep = ((float) view.getWidth()) / chart.getValuesX().size();
        maxY = 0;
        for (Chart.GraphData line:
             chart.getLines()) {
            if(maxY < line.getMaxValue()){
                maxY = line.getMaxValue();
            }
        }
        yStep = ((float) view.getHeight()) / maxY;
        yCenter = (float) view.getHeight() * 0.5f;
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

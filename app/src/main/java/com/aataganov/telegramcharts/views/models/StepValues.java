package com.aataganov.telegramcharts.views.models;

import android.view.View;

import com.aataganov.telegramcharts.models.Chart;

import java.util.Collections;

public class StepValues {
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

    public float getxStep() {
        return xStep;
    }

    public float getyStep() {
        return yStep;
    }

    public float getMaxY() {
        return maxY;
    }
}

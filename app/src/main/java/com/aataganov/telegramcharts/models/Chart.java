package com.aataganov.telegramcharts.models;

import android.graphics.Color;

import com.aataganov.telegramcharts.utils.ChartHelper;

import java.util.Collections;
import java.util.List;

public class Chart {
    List<Long> valuesX;
    List<GraphData> graphsList;
    int maxY;

    public Chart(List<Long> xValues, List<GraphData> graphs) {
        valuesX = xValues;
        graphsList = graphs;
        maxY = ChartHelper.calculateMaxY(this);
    }

    public int getMaxY() {
        return maxY;
    }

    public List<Long> getValuesX() {
        return valuesX;
    }

    public List<GraphData> getGraphsList() {
        return graphsList;
    }

    public static class GraphData{
        List<Integer> values;
        String type;
        String name;
        int color;
        int maxValue;

        public GraphData(List<Integer> values, String type, String name, String color) {
            this.values = values;
            this.type = type;
            this.name = name;
            this.color = Color.parseColor(color);
            maxValue = Collections.max(values);
        }

        public List<Integer> getValues() {
            return values;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public int getColor() {
            return color;
        }

        public int getMaxValue() {
            return maxValue;
        }
    }
}

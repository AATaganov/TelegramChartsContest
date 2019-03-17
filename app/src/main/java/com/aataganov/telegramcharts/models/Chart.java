package com.aataganov.telegramcharts.models;

import android.graphics.Color;

import java.util.Collections;
import java.util.List;

public class Chart {
    List<Long> valuesX;
    List<GraphData> graphsList;

    public Chart(List<Long> xValues, List<GraphData> graphs) {
        valuesX = xValues;
        graphsList = graphs;
    }

    public List<Long> getValuesX() {
        return valuesX;
    }

    public List<GraphData> getGraphsList() {
        return graphsList;
    }

    public static class GraphData{
        List<Long> values;
        String type;
        String name;
        int color;
        long maxValue;

        public GraphData(List<Long> values, String type, String name, String color) {
            this.values = values;
            this.type = type;
            this.name = name;
            this.color = Color.parseColor(color);
            maxValue = Collections.max(values);
        }

        public GraphData(List<Long> values, String name) {
            this.values = values;
            this.name = name;
        }

        public List<Long> getValues() {
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

        public long getMaxValue() {
            return maxValue;
        }
    }
}

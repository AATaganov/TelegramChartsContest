package com.aataganov.telegramcharts.utils;

import android.graphics.Path;

import com.aataganov.telegramcharts.models.Chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChartHelper {
    public static Path drawChart(List<Long> values,
                                 float yShift,
                                 float xStep,
                                 float yStep){
        Path path = new Path();
        if(values.size() < 2){
            path.close();
            return path;
        }
        int length = values.size();
        float currentX = 0;
        float currentY = yShift - (values.get(0) * yStep);
        for(int index = 1; index < length; ++index){
            float newY = yShift - (values.get(index) * yStep);
            float newX = currentX + xStep;
            addLineToPath(path,currentX,currentY, newX, newY);
            currentX = newX;
            currentY = newY;
        }
        path.close();
        return path;
    }

    private static void addLineToPath(Path path, float startX, float startY, float endX, float endY){
        path.moveTo(startX,startY);
        path.lineTo(endX, endY);
    }
    public static List<Boolean> copySelectionList(List<Boolean> originalList){
        List<Boolean> result = new ArrayList<>(originalList);
        Collections.copy(originalList, result);
        return result;
    }
    public static float calculateMaxY(Chart chart, List<Boolean> selectionList){
        float maxY = 0;
        int graphsSize = chart.getGraphsList().size();
        for (int index = 0; index < graphsSize; ++index) {
            Chart.GraphData graph = chart.getGraphsList().get(index);
            if(selectionList.get(index) && maxY < graph.getMaxValue()){
                maxY = graph.getMaxValue();
            }
        }
        return maxY;
    }
    public static float calculateMaxY(Chart chart){
        float maxY = 0;
        int graphsSize = chart.getGraphsList().size();
        for (int index = 0; index < graphsSize; ++index) {
            Chart.GraphData graph = chart.getGraphsList().get(index);
            if(maxY < graph.getMaxValue()){
                maxY = graph.getMaxValue();
            }
        }
        return maxY;
    }
}

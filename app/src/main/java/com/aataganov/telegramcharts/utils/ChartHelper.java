package com.aataganov.telegramcharts.utils;

import android.graphics.Path;

import com.aataganov.telegramcharts.models.Chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.aataganov.telegramcharts.helpers.Constants.FULL_ALPHA;

public class ChartHelper {
    public static Path buildGraphPath(List<Integer> values,
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

    public static void addLineToPath(Path path, float startX, float startY, float endX, float endY){
        path.moveTo(startX,startY);
        path.lineTo(endX, endY);
    }
    public static List<Boolean> copySelectionList(List<Boolean> originalList){
        List<Boolean> result = new ArrayList<>(originalList);
        Collections.copy(originalList, result);
        return result;
    }

    public static int calculateMaxY(Chart chart, List<Boolean> selectionList, int startIndex, int endIndex){
        int maxY = 0;
        int graphsSize = Math.min(chart.getValuesX().size(), endIndex);
        for (int index = startIndex; index < graphsSize; ++index) {
            if(selectionList.get(index)){
                continue;
            }
            Chart.GraphData graph = chart.getGraphsList().get(index);
            List<Integer> sublist = graph.getValues().subList(startIndex, endIndex);
            Integer chartMaxY = Collections.max(sublist);
            if(maxY < chartMaxY){
                maxY = chartMaxY;
            }
        }
        return maxY;
    }

    public static int calculateMaxY(Chart chart, List<Boolean> selectionList){
        int maxY = 0;
        int graphsSize = chart.getGraphsList().size();
        for (int index = 0; index < graphsSize; ++index) {
            Chart.GraphData graph = chart.getGraphsList().get(index);
            if(selectionList.get(index) && maxY < graph.getMaxValue()){
                maxY = graph.getMaxValue();
            }
        }
        return maxY;
    }
    public static int calculateMaxY(Chart chart){
        int maxY = 0;
        int graphsSize = chart.getGraphsList().size();
        for (int index = 0; index < graphsSize; ++index) {
            Chart.GraphData graph = chart.getGraphsList().get(index);
            if(maxY < graph.getMaxValue()){
                maxY = graph.getMaxValue();
            }
        }
        return maxY;
    }

    public static int calculateTransitionAlpha(long lvl, int maxCount){
        if(lvl >= maxCount){
            return FULL_ALPHA;
        }
        return (int) (FULL_ALPHA * lvl / maxCount);
    }

    public static int calculateStep(int itemsCount, int stepsCount){
        if(itemsCount <= stepsCount){
            return 1;
        }
        int result = 2;
        while (stepsCount * result < itemsCount){
            result *= 2;
        }
        return result;
    }
}

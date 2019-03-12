package com.aataganov.telegramcharts.utils;

import android.graphics.Path;

import java.util.List;

public class ChartHelper {
    public static Path drawChart(List<Long> values,
                                 float maxY,
                                 float xStep,
                                 float yStep){
        Path path = new Path();
        if(values.size() < 2){
            path.close();
            return path;
        }
        int length = values.size();
        float currentX = 0;
        float currentY = (maxY - values.get(0)) * yStep;
        for(int index = 1; index < length; ++index){
            float newY = (maxY - values.get(index)) * yStep;
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
}

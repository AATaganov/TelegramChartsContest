package com.aataganov.telegramcharts.views.models;

public class ChartDiapason {
    int startIndex;
    int endIndex;

    public ChartDiapason(int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int getItemsInDiapason(){
        return endIndex - startIndex;
    }
}

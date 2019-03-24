package com.aataganov.telegramcharts.views.models;

public class ChartDiapason {
    int startIndex;
    int endIndex;
    int itemsInDiapason;
    private float startShift;
    private float endShift;
    private int itemsCount;
    private float selectedDiapasonWidth;
    private float wholeDiapasonWidth;
    private float previewItemWidth;
    private DrawChartValues chartValues;

    public ChartDiapason(int startIndex, int endIndex, float startShift, float endShift, float selectedDiapasonWidth, int itemsCount) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.startShift = startShift;
        this.endShift = endShift;
        this.itemsCount = itemsCount;
        itemsInDiapason = endIndex - startIndex;
        this.selectedDiapasonWidth = selectedDiapasonWidth;
        wholeDiapasonWidth = selectedDiapasonWidth + startShift + endShift;
        previewItemWidth = wholeDiapasonWidth / itemsInDiapason;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int getItemsInDiapason(){
        return itemsInDiapason;
    }

    public int getItemsCount() {
        return itemsCount;
    }

    public DrawChartValues getDrawChartValues(int chartWidth){
        if(chartValues == null || chartValues.chartWidth != chartWidth){
            float proportion = (float) chartWidth / selectedDiapasonWidth;
            float step = previewItemWidth * proportion;
            float offset = startShift * proportion;
            chartValues = new DrawChartValues(offset, step, chartWidth);
        }
        return chartValues;
    }

    public class DrawChartValues{
        float offset;
        float step;
        private int chartWidth;

        public DrawChartValues(float offset, float step,int chartWidth) {
            this.offset = offset;
            this.step = step;
            this.chartWidth = chartWidth;
        }

        public float getOffset() {
            return offset;
        }

        public float getStep() {
            return step;
        }
    }
}

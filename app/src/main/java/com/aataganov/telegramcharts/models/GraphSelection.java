package com.aataganov.telegramcharts.models;

public class GraphSelection {
    private boolean selectedY0;
    private boolean selectedY1;

    public GraphSelection(boolean selectedY0,
                          boolean selectedY1) {
        this.selectedY0 = selectedY0;
        this.selectedY1 = selectedY1;
    }
}

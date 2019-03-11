package com.aataganov.telegramcharts.models;

import com.aataganov.telegramcharts.gson.GsonAxisValues;
import com.aataganov.telegramcharts.gson.GsonTypes;

import java.util.ArrayList;
import java.util.List;

public class Chart {
    List<Long> valuesX = new ArrayList<>();
    List<Integer> valuesY0 = new ArrayList<>();
    List<Integer> valuesY1 = new ArrayList<>();
    GsonTypes types;
    GsonAxisValues names;
    GsonAxisValues colors;

    public Chart(List<Long> valuesX,
                 List<Integer> valuesY0,
                 List<Integer> valuesY1,
                 GsonTypes types,
                 GsonAxisValues names,
                 GsonAxisValues colors) {
        this.valuesX = valuesX;
        this.valuesY0 = valuesY0;
        this.valuesY1 = valuesY1;
        this.types = types;
        this.names = names;
        this.colors = colors;
    }


    public List<Long> getValuesX() {
        return valuesX;
    }

    public List<Integer> getValuesY0() {
        return valuesY0;
    }

    public List<Integer> getValuesY1() {
        return valuesY1;
    }

    public GsonTypes getTypes() {
        return types;
    }

    public GsonAxisValues getNames() {
        return names;
    }

    public GsonAxisValues getColors() {
        return colors;
    }
}

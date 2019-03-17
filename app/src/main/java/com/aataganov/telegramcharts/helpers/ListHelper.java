package com.aataganov.telegramcharts.helpers;

import android.widget.BaseAdapter;

import java.util.List;

public class ListHelper {
    public static final int EMPTY_ID = -1;

    public static boolean isNullOrEmpty(List list) {
        return (list == null || list.isEmpty());
    }

    public static boolean hasItems(List list) {
        return !isNullOrEmpty(list);
    }

    public static boolean isOutOfBounds(List list, int index){
        return (index < 0 || list == null || index >= list.size());
    }
}

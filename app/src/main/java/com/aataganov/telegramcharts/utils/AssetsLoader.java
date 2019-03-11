package com.aataganov.telegramcharts.utils;

import android.content.Context;

import com.aataganov.telegramcharts.helpers.AssetHelper;
import com.aataganov.telegramcharts.helpers.JsonParseHelper;
import com.aataganov.telegramcharts.models.Chart;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class AssetsLoader {
    private static final String JSON_FILENAME_CHARTS = "chart_data.json";

    public static Single<List<Chart>> loadCharts(final Context context){
        return Single.fromCallable(() -> AssetHelper.loadJSONFromAsset(context, JSON_FILENAME_CHARTS))
                .subscribeOn(Schedulers.computation())
                .map(JsonParseHelper::parseChartListJson)
                .observeOn(AndroidSchedulers.mainThread());
    }
}

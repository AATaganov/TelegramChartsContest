package com.aataganov.telegramcharts.helpers;

import com.aataganov.telegramcharts.gson.GsonAxisValues;
import com.aataganov.telegramcharts.gson.GsonTypes;
import com.aataganov.telegramcharts.models.Chart;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonParseHelper {
    private static final int X_VALUES_INDEX = 0;
    private static final int Y0_VALUES_INDEX = 1;
    private static final int Y1_VALUES_INDEX = 2;

    private static final String FIELD_COLUMNS = "columns";
    private static final String FIELD_TYPES = "types";
    private static final String FIELD_NAMES = "names";
    private static final String FIELD_COLORS = "colors";

    public static List<Chart> parseChartListJson(String jsonString) throws JSONException {
        JSONArray jsonArray = new JSONArray(jsonString);
        Gson gson = new Gson();
        List<Chart> result = new ArrayList<>();
        int length = jsonArray.length();
        for(int index = 0; index < length; ++index){
            JSONObject jsonChart = jsonArray.getJSONObject(index);
            result.add(parseSingleChartJson(jsonChart, gson));
        }
        return result;
    }

    private static Chart parseSingleChartJson(JSONObject jsonObject, Gson gson) throws JSONException {
        JSONArray columns = jsonObject.getJSONArray(FIELD_COLUMNS);
        List<Long> valuesX = parseLongValues(columns.getJSONArray(X_VALUES_INDEX));
        List<Integer> valuesY0 = parseIntegerValues(columns.getJSONArray(Y0_VALUES_INDEX));
        List<Integer> valuesY1 = parseIntegerValues(columns.getJSONArray(Y1_VALUES_INDEX));
        GsonTypes types = gson.fromJson(jsonObject.getString(FIELD_TYPES), GsonTypes.class);
        GsonAxisValues names = gson.fromJson(jsonObject.getString(FIELD_NAMES), GsonAxisValues.class);
        GsonAxisValues colors = gson.fromJson(jsonObject.getString(FIELD_COLORS), GsonAxisValues.class);
        return new Chart(valuesX, valuesY0, valuesY1, types, names, colors);
    }
    private static List<Long> parseLongValues(JSONArray json) throws JSONException {
        List<Long> result = new ArrayList<>();
        int length = json.length();
        for(int index = 1; index < length; ++index){
            result.add(json.getLong(index));
        }
        return result;
    }
    private static List<Integer> parseIntegerValues(JSONArray json) throws JSONException {
        List<Integer> result = new ArrayList<>();
        int length = json.length();
        for(int index = 1; index < length; ++index){
            result.add(json.getInt(index));
        }
        return result;
    }
}

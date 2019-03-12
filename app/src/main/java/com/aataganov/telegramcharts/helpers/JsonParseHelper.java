package com.aataganov.telegramcharts.helpers;

import com.aataganov.telegramcharts.models.Chart;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonParseHelper {

    private static final String FIELD_COLUMNS = "columns";
    private static final String FIELD_TYPES = "types";
    private static final String FIELD_NAMES = "names";
    private static final String FIELD_COLORS = "colors";
    private static final String TYPE_X = "x";

    public static List<Chart> parseChartListJson(String jsonString) throws JSONException {
        JSONArray jsonArray = new JSONArray(jsonString);
        List<Chart> result = new ArrayList<>();
        int length = jsonArray.length();
        for(int index = 0; index < length; ++index){
            JSONObject jsonChart = jsonArray.getJSONObject(index);
            result.add(parseSingleChartJson(jsonChart));
        }
        return result;
    }

    private static Chart parseSingleChartJson(JSONObject jsonObject) throws JSONException {
        JSONArray columns = jsonObject.getJSONArray(FIELD_COLUMNS);
        List<Chart.GraphData> graphs = new ArrayList<>();
        List<Long> xValues = null;
        int columnsLength = columns.length();

        JSONObject jsonTypes = jsonObject.getJSONObject(FIELD_TYPES);
        JSONObject jsonNames = jsonObject.getJSONObject(FIELD_NAMES);
        JSONObject jsonColors = jsonObject.getJSONObject(FIELD_COLORS);
        for(int index = 0; index < columnsLength; ++index){
            JSONArray columnJson = columns.getJSONArray(index);
            String key = parseGraphKey(columnJson);
            List<Long> values = parseLongValues(columnJson);
            String typeString = jsonTypes.getString(key);

            if(typeString.equals(TYPE_X)){
                xValues = values;
                continue;
            }
            String name = jsonNames.getString(key);
            String color = jsonColors.getString(key);
            graphs.add(new Chart.GraphData(values,typeString,name,color));
        }
        return new Chart(xValues, graphs);
    }

    private static String parseGraphKey(JSONArray json) throws JSONException {
        return json.getString(0);
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

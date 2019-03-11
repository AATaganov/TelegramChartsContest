package com.aataganov.telegramcharts.helpers;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

public class AssetHelper {
    public static String loadJSONFromAsset(Context context, String assetFile) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(assetFile);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}

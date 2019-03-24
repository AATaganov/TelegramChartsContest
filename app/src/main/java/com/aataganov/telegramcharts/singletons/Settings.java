package com.aataganov.telegramcharts.singletons;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

public class Settings {

    private static String SETTING_NIGHT_MODE = "NightMode";


    private final SharedPreferences prefs;
    private boolean isNightModeOn;

    private static Settings instance = null;
    public static @NonNull
    Settings getInstance(Application application){
        if(instance == null){
            instance = new Settings(application);
        }
        return instance;
    }

    private Settings(Application application) {
        prefs = application.getSharedPreferences(SETTING_NIGHT_MODE, Context.MODE_PRIVATE);
        isNightModeOn = prefs.getBoolean(SETTING_NIGHT_MODE,false);
    }

    public void changeNightMode(){
        isNightModeOn = !isNightModeOn;
        putInPrefs(SETTING_NIGHT_MODE, isNightModeOn);
    }

    private void putInPrefs(String prefsName,boolean value){
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(prefsName,value);
        edit.apply();
    }

    public boolean isNightModeOn() {
        return isNightModeOn;
    }
}

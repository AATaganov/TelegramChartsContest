package com.aataganov.telegramcharts.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateHelper {
    public static SimpleDateFormat getShortDayFormat(){
        return new SimpleDateFormat(("MMM d"), Locale.ENGLISH);
    }
    public static String getShortDayString(Date date){
        SimpleDateFormat sdf = getShortDayFormat();
        return sdf.format(date);
    }

    public static SimpleDateFormat getDayOfMonthFormat(){
        return new SimpleDateFormat(("EEE, MMM d"), Locale.ENGLISH);
    }
    public static String getDayOfMonthString(Date date){
        SimpleDateFormat sdf = getDayOfMonthFormat();
        return sdf.format(date);
    }
}

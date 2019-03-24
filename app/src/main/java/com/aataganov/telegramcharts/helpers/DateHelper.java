package com.aataganov.telegramcharts.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateHelper {
    public static SimpleDateFormat getDayOfMonthFormat(){
        return new SimpleDateFormat(("MMM d"), Locale.getDefault());
    }
    public static String getShortDayString(Date date){
        SimpleDateFormat sdf = getDayOfMonthFormat();
        return sdf.format(date);
    }
}

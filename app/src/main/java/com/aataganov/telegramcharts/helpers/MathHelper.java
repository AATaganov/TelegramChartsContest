package com.aataganov.telegramcharts.helpers;

public class MathHelper {

    public static boolean isInRange(float value, float range){
        return (value <= range && value >= -range);
    }
    public static boolean isOutOfRange(float value, float range){
        return (value > range || value < -range);
    }
}

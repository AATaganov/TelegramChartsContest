package com.aataganov.telegramcharts.helpers;

public class MathHelper {

    public static boolean isInRange(float value, float range){
        return (value <= range && value >= -range);
    }

    public static boolean isInRange(int value, int start, int end){
        return (value >= start && value <= end);
    }

    public static int floorNumberToFirstToDigits(int number){
        for(int power = 8; power > 3; --power){
            int tensPower = ((Double) Math.pow(10, power)).intValue();
            if(number > tensPower){
                int twoDigitsShiftPower = ((Double) Math.pow(10, power - 2)).intValue();
                int reminder = number % twoDigitsShiftPower;
                return number - reminder;
            }
        }
        return number;
    }
}

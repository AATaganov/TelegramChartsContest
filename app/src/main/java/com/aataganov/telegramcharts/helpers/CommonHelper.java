package com.aataganov.telegramcharts.helpers;

import android.content.res.ColorStateList;
import android.os.Build;
import android.support.v4.widget.CompoundButtonCompat;
import android.widget.CheckBox;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;

public class CommonHelper {


    public static void unsubscribeDisposable(Disposable disposable){
        if(isDisposed(disposable)){
            return;
        }
        disposable.dispose();
    }
    public static void unsubscribeDisposeBag(CompositeDisposable disposable){
        if(isDisposed(disposable)){
            return;
        }
        disposable.dispose();
    }
    public static boolean isDisposed(CompositeDisposable disposable) {
        return (disposable == null || disposable.isDisposed());
    }
    public static boolean isDisposed(Disposable disposable){
        return (disposable == null || disposable.isDisposed());
    }
    public static boolean isDisposed(DisposableObserver observer){
        return (observer == null || observer.isDisposed());
    }

    public static void updateCheckboxColor(int color, CheckBox checkBox){
        if (Build.VERSION.SDK_INT < 21) {
            CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(color));//Use android.support.v4.widget.CompoundButtonCompat when necessary else
        } else {
            checkBox.setButtonTintList(ColorStateList.valueOf(color));//setButtonTintList is accessible directly on API>19
        }
    }
}

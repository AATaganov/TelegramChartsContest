package com.aataganov.telegramcharts.helpers;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.util.TypedValue;
import android.view.View;
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

    public static int calculateSizeWithPadding(int size, int padding){
        return Math.max(0, size - padding - padding);
    }
    @ColorInt
    public static int resolveColorAttr(Context context, @AttrRes int colorAttr) {
        TypedValue resolvedAttr = resolveThemeAttr(context, colorAttr);
        // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
        int colorRes = resolvedAttr.resourceId != 0 ? resolvedAttr.resourceId : resolvedAttr.data;
        return ContextCompat.getColor(context, colorRes);
    }
    public static TypedValue resolveThemeAttr(Context context, @AttrRes int attrRes) {
        Resources.Theme theme = context.getTheme();
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attrRes, typedValue, true);
        return typedValue;
    }

    public static void updateViewVisibility(View view, boolean show){
        if(show){
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }
}

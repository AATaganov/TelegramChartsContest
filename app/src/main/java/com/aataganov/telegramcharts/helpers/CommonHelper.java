package com.aataganov.telegramcharts.helpers;

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
}

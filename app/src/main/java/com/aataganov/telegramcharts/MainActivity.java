package com.aataganov.telegramcharts;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.aataganov.telegramcharts.helpers.AssetHelper;
import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.utils.AssetsLoader;

import io.reactivex.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    CompositeDisposable longLiveBag = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        loadCharts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CommonHelper.unsubscribeDisposeBag(longLiveBag);
    }

    private void loadCharts(){
        longLiveBag.add(AssetsLoader.loadCharts(this).subscribe(
                data -> {
                    Log.w(LOG_TAG,"Data: " + data.toString());
                },
                error -> {
                    error.printStackTrace();
                }
        ));
    }


}

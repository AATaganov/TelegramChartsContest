package com.aataganov.telegramcharts;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.aataganov.telegramcharts.helpers.AssetHelper;
import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.utils.AssetsLoader;
import com.aataganov.telegramcharts.views.ViewChartDiapasonPicker;

import io.reactivex.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    CompositeDisposable longLiveBag = new CompositeDisposable();
    ViewChartDiapasonPicker chartDiapasonPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    void initViews(){
        chartDiapasonPicker = findViewById(R.id.view_diapason_picker);
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
                    chartDiapasonPicker.setChart(data.get(0));
                },
                Throwable::printStackTrace
        ));
    }


}

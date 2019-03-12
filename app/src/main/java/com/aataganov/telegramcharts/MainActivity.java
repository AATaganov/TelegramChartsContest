package com.aataganov.telegramcharts;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.models.Chart;
import com.aataganov.telegramcharts.models.GraphSelection;
import com.aataganov.telegramcharts.utils.AssetsLoader;
import com.aataganov.telegramcharts.views.ViewChartDiapasonPicker;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    CompositeDisposable longLiveBag = new CompositeDisposable();
    ViewChartDiapasonPicker chartDiapasonPicker;
    CheckBox checkBoxY0;
    CheckBox checkBoxY1;

    BehaviorSubject<GraphSelection> graphSelectionSubject = BehaviorSubject.createDefault(new GraphSelection(true,true));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    void initViews(){
        chartDiapasonPicker = findViewById(R.id.view_diapason_picker);
        checkBoxY0 = findViewById(R.id.checkBoxY0);
        checkBoxY1 = findViewById(R.id.checkBoxY1);
        checkBoxY0.setOnCheckedChangeListener(this);
        checkBoxY1.setOnCheckedChangeListener(this);
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
                    setNewChart(data.get(4));
                },
                Throwable::printStackTrace
        ));
    }

    private void setNewChart(Chart chart){
        chartDiapasonPicker.setChart(chart);
//        checkBoxY0.setText(chart.getTypes().getY0());
//        checkBoxY1.setText(chart.getTypes().getY1());
//        CommonHelper.updateCheckboxColor(chart.getColorY0(), checkBoxY0);
//        CommonHelper.updateCheckboxColor(chart.getColorY1(), checkBoxY1);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateGraphicsSelectionSubject();
    }

    private void updateGraphicsSelectionSubject() {
        graphSelectionSubject.onNext(new GraphSelection(checkBoxY0.isChecked(), checkBoxY1.isChecked()));
    }
}

package com.aataganov.telegramcharts;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.aataganov.telegramcharts.adapters.AdapterChartsSelection;
import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.models.Chart;
import com.aataganov.telegramcharts.utils.AssetsLoader;
import com.aataganov.telegramcharts.views.ViewChartDiapasonPicker;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

public class MainActivity extends AppCompatActivity implements AdapterChartsSelection.SelectionListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    CompositeDisposable longLiveBag = new CompositeDisposable();
    ViewChartDiapasonPicker chartDiapasonPicker;
    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;

    AdapterChartsSelection adapterChartsSelection = new AdapterChartsSelection();

    BehaviorSubject<List<Boolean>> graphSelectionSubject = BehaviorSubject.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initRecycler();
    }

    void initViews(){
        chartDiapasonPicker = findViewById(R.id.view_diapason_picker);
        recyclerView = findViewById(R.id.recycler_selection_checkboxes);
        initRecycler();
    }

    void initRecycler() {
        layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        adapterChartsSelection.setSelectionListener(this);
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
        adapterChartsSelection.updateData(chart.getLines());
        if(recyclerView.getAdapter() == null){
            recyclerView.setAdapter(adapterChartsSelection);
        }
    }


    @Override
    public void onSelectionChanged(List<Boolean> selectionList) {
        graphSelectionSubject.onNext(selectionList);
    }
}

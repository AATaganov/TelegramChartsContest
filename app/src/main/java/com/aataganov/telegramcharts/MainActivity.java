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
import com.aataganov.telegramcharts.utils.ChartHelper;
import com.aataganov.telegramcharts.views.ViewChart;
import com.aataganov.telegramcharts.views.ViewChartDiapasonPicker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class MainActivity extends AppCompatActivity implements AdapterChartsSelection.SelectionListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    CompositeDisposable longLiveBag = new CompositeDisposable();
    CompositeDisposable activeBag = new CompositeDisposable();
    ViewChartDiapasonPicker chartDiapasonPicker;
    ViewChart chartView;
    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;

    AdapterChartsSelection adapterChartsSelection = new AdapterChartsSelection();

    BehaviorSubject<List<Boolean>> graphSelectionSubject = BehaviorSubject.create();
    private List<Boolean> selectionList = new ArrayList<>();
    private Chart selectedChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initRecycler();
    }

    void initViews(){
        chartView = findViewById(R.id.chart_view);
        chartDiapasonPicker = findViewById(R.id.view_diapason_picker);
        chartView.setPicker(chartDiapasonPicker);
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
    protected void onStart() {
        super.onStart();
        if(CommonHelper.isDisposed(activeBag)){
            activeBag = new CompositeDisposable();
        }
        subscribeToSelectionUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommonHelper.unsubscribeDisposeBag(activeBag);
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
                    setNewChart(data.get(0));
                },
                Throwable::printStackTrace
        ));
    }

    private void setNewChart(Chart chart){
        chartView.stopListeningDiapasonChanges();
        selectedChart = chart;
        for (Chart.GraphData ignored :
                chart.getGraphsList()) {
            selectionList.add(true);
        }
        chartDiapasonPicker.setChart(chart, selectionList);
        adapterChartsSelection.updateData(selectedChart.getGraphsList(),selectionList);
        if(recyclerView.getAdapter() == null){
            recyclerView.setAdapter(adapterChartsSelection);
        }
        chartView.setChart(selectedChart,selectionList);
        chartView.subscribeToDiapasonChanges();
    }

    @Override
    public void onSelectionChanged(int position) {
        boolean newValue = !selectionList.get(position);
        selectionList.set(position,newValue);
        graphSelectionSubject.onNext(selectionList);
    }

    public void subscribeToSelectionUpdates(){
        longLiveBag.add(graphSelectionSubject.throttleLatest(600, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(selection -> ChartHelper.copySelectionList(selectionList))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    chartDiapasonPicker.setNewSelection(res);
                    chartView.setNewSelection(res);

                }, error -> {error.printStackTrace();}));
    }
}

package com.aataganov.telegramcharts;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.aataganov.telegramcharts.adapters.AdapterChartsSelection;
import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.helpers.ListHelper;
import com.aataganov.telegramcharts.models.Chart;
import com.aataganov.telegramcharts.singletons.Settings;
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

    Button btnNext;
    Button btnPrevious;
//    Toolbar toolbar;

    AdapterChartsSelection adapterChartsSelection = new AdapterChartsSelection();

    BehaviorSubject<List<Boolean>> graphSelectionSubject = BehaviorSubject.create();
    private List<Boolean> selectionList = new ArrayList<>();
    private Chart selectedChart;
    private BehaviorSubject<Chart> chartSubject = BehaviorSubject.create();
    private List<Chart> chartsList;
    private int currentChartIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        updateActivityTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initRecycler();
        initButtons();
        initToolbar();
    }
    private void updateActivityTheme(){
        Settings settings = Settings.getInstance(getApplication());
        if(settings.isNightModeOn()) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void initToolbar(){
//        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.activity_title);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }
    public void restart(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_switch_theme){
            switchTheme();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void switchTheme() {
        Settings.getInstance(getApplication()).changeNightMode();
        restart();
    }

    private void subscribeToChartSelection(){
        activeBag.add(chartSubject.throttleLatest(1000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(chart -> {
                    if(chart != selectedChart){
                        chartView.clearChart();
                        chartDiapasonPicker.clearChart();
                    }
                    return chart;
                })
                .delay(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(chart -> {
                    if(selectedChart != chart){
                        setNewChart(chart);
                    }
                }, error -> {error.printStackTrace();}));
    }

    private void initButtons() {
        btnNext.setOnClickListener(v -> {
            selectChartIndex(currentChartIndex + 1);
        });
        btnPrevious.setOnClickListener(v -> {
            selectChartIndex(currentChartIndex - 1);
        });
    }

    void initViews(){
//        toolbar = findViewById(R.id.toolbar);
        chartView = findViewById(R.id.chart_view);
        chartDiapasonPicker = findViewById(R.id.view_diapason_picker);
        chartView.setPicker(chartDiapasonPicker);
        recyclerView = findViewById(R.id.recycler_selection_checkboxes);
        btnNext = findViewById(R.id.btn_next_chart);
        btnPrevious = findViewById(R.id.btn_previous_chart);
        initRecycler();
        initButtons();
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
        subscribeToChartSelection();
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
                    chartsList = data;
                    selectChartIndex(0);
                },
                Throwable::printStackTrace
        ));
    }

    private void selectChartIndex(int newIndex) {
        if(ListHelper.isOutOfBounds(chartsList,newIndex)){
            return;
        }
        currentChartIndex = newIndex;
        btnPrevious.setEnabled(newIndex > 0);
        btnNext.setEnabled(newIndex < chartsList.size() - 1);
        chartSubject.onNext(chartsList.get(newIndex));
    }

    private void setNewChart(Chart chart){
        chartView.stopListeningDiapasonChanges();
        selectedChart = chart;
        selectionList.clear();
        selectionList = ChartHelper.buildFullSelectedList(chart.getGraphsList().size());
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
        activeBag.add(graphSelectionSubject.throttleLatest(600, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(selection -> ChartHelper.copySelectionList(selectionList))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(res -> {
                    chartDiapasonPicker.setNewSelection(res);
                    chartView.setNewSelection(res);

                }, error -> {error.printStackTrace();}));
    }
}

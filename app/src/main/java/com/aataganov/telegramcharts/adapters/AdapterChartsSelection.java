package com.aataganov.telegramcharts.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.aataganov.telegramcharts.R;
import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.models.Chart;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AdapterChartsSelection extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Chart.GraphData> dataList = new ArrayList<>();
    private List<Boolean> selectionList = new ArrayList<>();
    WeakReference<SelectionListener> weakListener;

    public void setSelectionListener(SelectionListener listener) {
        weakListener = new WeakReference<>(listener);
    }

    public void updateData(List<Chart.GraphData> newList){
        dataList = newList;
        selectionList.clear();
        for (Chart.GraphData ignored :
             dataList) {
            selectionList.add(true);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_item_graph_selection, viewGroup, false);
        return new SelectionCheckboxViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof SelectionCheckboxViewHolder){
            ((SelectionCheckboxViewHolder) holder).update(position);
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    private class SelectionCheckboxViewHolder extends RecyclerView.ViewHolder{

        private final CheckBox checkBox;
        public SelectionCheckboxViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox);
            checkBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                boolean newValue = !selectionList.get(position);
                checkBox.setChecked(newValue);
                selectionList.set(position,newValue);
                notifyItemChanged(position);
                if(weakListener.get() != null){
//                    List<Boolean> newList = new ArrayList<>();
//                    Collections.copy(selectionList,newList);
                    weakListener.get().onSelectionChanged(selectionList);
                }
            });
        }

        public void update(int position){
            Chart.GraphData item = dataList.get(position);
            CommonHelper.updateCheckboxColor(item.getColor(), checkBox);
            checkBox.setChecked(selectionList.get(position));
            checkBox.setText(item.getName());
        }
    }

    public interface SelectionListener{
        void onSelectionChanged(List<Boolean> selectionList);
    }
}
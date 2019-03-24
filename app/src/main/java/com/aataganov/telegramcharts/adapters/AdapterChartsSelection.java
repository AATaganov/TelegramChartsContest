package com.aataganov.telegramcharts.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.aataganov.telegramcharts.R;
import com.aataganov.telegramcharts.helpers.CommonHelper;
import com.aataganov.telegramcharts.models.Chart;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AdapterChartsSelection extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Chart.GraphData> dataList = new ArrayList<>();
    private List<Boolean> selectionList = new ArrayList<>();
    private WeakReference<SelectionListener> weakListener;

    public void setSelectionListener(SelectionListener listener) {
        weakListener = new WeakReference<>(listener);
    }

    public void updateData(List<Chart.GraphData> newList,List<Boolean> newSelectionList){
        dataList = newList;
        selectionList = newSelectionList;
        notifyDataSetChanged();
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
        private final TextView txtTitle;
        private final View divider;
        SelectionCheckboxViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox);
            txtTitle = itemView.findViewById(R.id.txt_title);
            divider = itemView.findViewById(R.id.view_divider);
            checkBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                weakListener.get().onSelectionChanged(position);
                notifyItemChanged(position);
            });
        }

        void update(int position){
            Chart.GraphData item = dataList.get(position);
            CommonHelper.updateCheckboxColor(item.getColor(), checkBox);
            checkBox.setChecked(selectionList.get(position));
            txtTitle.setText(item.getName());
            CommonHelper.updateViewVisibility(divider,position < dataList.size() - 1);
        }
    }

    public interface SelectionListener{
        void onSelectionChanged(int adapterPosition);
    }
}

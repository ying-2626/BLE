package com.example.backpackapplication.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.backpackapplication.databinding.ItemActionBinding;
import com.example.backpackapplication.util.model.RfidTagAction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ActionHistoryAdapter extends RecyclerView.Adapter<ActionHistoryAdapter.ViewHolder> {

    private List<RfidTagAction> actions = new ArrayList<>();

    public void updateData(List<RfidTagAction> newData) {
        actions.clear();
        if (newData != null) {
            actions.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemActionBinding binding = ItemActionBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RfidTagAction action = actions.get(position);

        String actionText = action.getAction() + "\n" +
                "时间: " + formatDateTime(action.getActionTime());

        holder.binding.actionText.setText(actionText);
    }

    private String formatDateTime(String isoTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(isoTime);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return isoTime;
        }
    }

    @Override
    public int getItemCount() {
        return actions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemActionBinding binding;

        ViewHolder(ItemActionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

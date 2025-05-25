package com.example.backpackapplication.adapter;

import static androidx.core.content.ContextCompat.startActivity;
import static com.example.backpackapplication.util.NetworkUtil.post;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.backpackapplication.LoginActivity;
import com.example.backpackapplication.R;
import com.example.backpackapplication.RegisterActivity;
import com.example.backpackapplication.SearchTagActivity;
import com.example.backpackapplication.TagActionActivity;
import com.example.backpackapplication.databinding.ItemTagBinding;
import com.example.backpackapplication.util.NetworkUtil;
import com.example.backpackapplication.util.UserUtil;
import com.example.backpackapplication.util.model.RfidTag;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagItemAdapter extends RecyclerView.Adapter<TagItemAdapter.TagViewHolder>{
        private List<RfidTag> data = new ArrayList<>();
        private final Context context; // 添加上下文引用
        private final OnTagActionListener listener;

    public interface OnTagActionListener {
            void onDelete(RfidTag tag);
            void onEdit(RfidTag tag);
        }

    // 修改构造函数
    public TagItemAdapter(Context context,OnTagActionListener listener) {

        this.listener = listener;
        this.context = context;
    }

    public void updateData(List<RfidTag> newData) {
        data.clear();
        if (newData != null) { // 空数据保护
            data.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @Override
    public TagItemAdapter.TagViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        ItemTagBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(viewGroup.getContext()),
                R.layout.item_tag,
                viewGroup,
                false);

        return new TagItemAdapter.TagViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        RfidTag tag = data.get(position);
        // 添加空值检查
        String info = "标签ID: " + (tag.getRfidTagId() != null ? tag.getRfidTagId() : "N/A") +
                "\n物品名称: " + (tag.getItemName() != null ? tag.getItemName() : "未命名")+
                "\n物品状态："+(tag.getRfidTagStatus() == 1 ? "在包内" : "已取出");

        holder.binding.tagText.setText(info);

        holder.binding.tagDeleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(tag);
            }
        });
        holder.binding.tagHistoryButton.setOnClickListener(l -> {
            Intent intent = new Intent(context, TagActionActivity.class);
            intent.putExtra("rfidTagId", tag.getRfidTagId());
            context.startActivity(intent);
            });
        }

    @Override
    public int getItemCount() {
        return data.size();
    }


    protected static class TagViewHolder extends RecyclerView.ViewHolder{
        ItemTagBinding binding;
        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            this.binding = DataBindingUtil.bind(itemView);
        }
    }
}

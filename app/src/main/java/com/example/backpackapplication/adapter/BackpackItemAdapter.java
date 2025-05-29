package com.example.backpackapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.backpackapplication.R;
import com.example.backpackapplication.SearchTagActivity;
import com.example.backpackapplication.databinding.ItemBackpackBinding;
import com.example.backpackapplication.ui.dashboard.BackpackFragment; // 修改导入路径
import com.example.backpackapplication.util.BaseResponse;
import com.example.backpackapplication.util.NetworkUtil;
import com.example.backpackapplication.util.PagedResult;
import com.example.backpackapplication.util.model.Backpack;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.List;

public class BackpackItemAdapter extends RecyclerView.Adapter<BackpackItemAdapter.ViewHolder> {
    private final List<Backpack> data;
    //private final BackpackFragment fragment; // 改为Fragment引用
    private final WeakReference<BackpackFragment> fragmentRef; // 改用弱引用

    // 修改构造函数参数为Fragment
    public BackpackItemAdapter(List<Backpack> data, BackpackFragment fragment) {
        this.data = data;
        this.fragmentRef = new WeakReference<>(fragment);
    }
    // 新增：用于获取当前数据列表
    public List<Backpack> getData() {
        return data;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBackpackBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.item_backpack,
                parent,
                false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Backpack item = data.get(position);

        String info = buildBackpackInfo(item);
        holder.binding.backpackInfoText.setText(info);

        setupCheckTagButton(holder, item);
        setupDeleteButton(holder, position, item);
    }

    private String buildBackpackInfo(Backpack item) {
        return "背包名称: " + item.getBackpackName() + "\n"
                + "序列号: " + item.getBackpackId() + "\n"
                + "电量: " + String.format("%.1f%%", item.getBackpackBattery()) + "\n"
                + "标签数量: " + item.getRfidTagNum() + "\n"
                + "位置：" + item.getLocation();
    }

    private void setupCheckTagButton(@NonNull ViewHolder holder, Backpack item) {
        holder.binding.checkTagButton.setOnClickListener(v -> {
            // 获取 Fragment 引用
            BackpackFragment fragment = fragmentRef.get();

            Intent intent = new Intent(fragment.requireContext(), SearchTagActivity.class); // 使用Fragment的Context
            intent.putExtra("BACKPACK_ID", item.getBackpackId());
            fragment.requireActivity().startActivity(intent);
        });
    }

    private void setupDeleteButton(@NonNull ViewHolder holder, int position, Backpack item) {
        holder.binding.deleteBackpackButton.setOnClickListener(v -> {
            // 获取 Fragment 引用
            BackpackFragment fragment = fragmentRef.get();

            if (fragment == null || !fragment.isAdded() || fragment.isDetached()) {
                Log.e("BackpackItemAdapter", "删除取消：Fragment 不可用");
                return;
            }

            // 使用安全上下文获取方式
            Context context = fragment.getContext();
            if (context == null) return;

            // 通过Fragment获取SharedPreferences
            SharedPreferences sp = fragment.requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
            String sessionId = sp.getString("sessionId", "");
            String backpackId = item.getBackpackId();

            new Thread(() -> {
            try {
                Type responseType = new TypeToken<BaseResponse<PagedResult<Backpack>>>(){}.getType();
                BaseResponse<?> response = NetworkUtil.getWithSession(
                        NetworkUtil.baseURL + "backpack/deactivate?backpackId=" + item.getBackpackId(),
                        sessionId,
                        responseType
                );

                // 使用Fragment的Activity运行UI线程
                fragment.requireActivity().runOnUiThread(() -> handleDeleteResponse(response, position));
                fragment.syncBackpack(); // 调用Fragment的同步方法
            } catch (Exception e) {
                Log.e("BackpackItemAdapter", "删除请求失败", e);
                showError("请求错误: " + e.getMessage());
            }
        }).start();
    });
    }

    private void handleDeleteResponse(BaseResponse<?> response, int position) {
        BackpackFragment fragment = fragmentRef.get();
        if (fragment == null || !fragment.isAdded()) return;

        if (response.getCode() == 0) {
            data.remove(position);
            notifyItemRemoved(position);
            Toast.makeText(fragment.requireContext(), "背包删除成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(fragment.requireContext(), "删除失败: " + response, Toast.LENGTH_LONG).show();
            Log.d("BackpackData", "删除背包的完整响应: " + new Gson().toJson(response));
        }
    }

    private void showError(String message) {
        BackpackFragment fragment = fragmentRef.get();
        if (fragment == null || !fragment.isAdded()) return;

        fragment.requireActivity().runOnUiThread(() ->
                Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_LONG).show()
        );
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemBackpackBinding binding;

        ViewHolder(ItemBackpackBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
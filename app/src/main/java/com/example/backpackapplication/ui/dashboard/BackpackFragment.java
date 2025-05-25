
package com.example.backpackapplication.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.backpackapplication.adapter.BackpackItemAdapter;
import com.example.backpackapplication.databinding.FragmentBackpackBinding;
import com.example.backpackapplication.util.BaseResponse;
import com.example.backpackapplication.util.NetworkUtil;
import com.example.backpackapplication.util.PagedResult;
import com.example.backpackapplication.util.model.Backpack;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;

public class BackpackFragment extends Fragment {
    private FragmentBackpackBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBackpackBinding.inflate(inflater, container, false);

        // 设置Toolbar
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(binding.toolbar);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setTitle("我的背包列表");

        setupRecyclerView();
        setupActivateButton();
        syncBackpack();
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        binding.backpackRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupActivateButton() {
        binding.activateButton.setOnClickListener(v -> showActivationDialog());
    }

    private void showActivationDialog() {
        final EditText editText = new EditText(requireContext());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("输入背包名")
                .setView(editText)
                .setPositiveButton("确定", null) // 先设为null后续自定义
                .setNegativeButton("取消", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            positiveButton.setOnClickListener(v -> {
                String backpackName = editText.getText().toString().trim();

                if (backpackName.isEmpty()) {
                    // 显示错误提示且不关闭对话框
                    Toast.makeText(requireContext(), "背包名称不能为空", Toast.LENGTH_SHORT).show();
                } else {
                    activateBackpack(backpackName);
                    dialog.dismiss(); // 手动关闭对话框
                }
            });
        });

        dialog.show();
    }

    public void syncBackpack() {
        new Thread(() -> {
            try {
                SharedPreferences sp = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
                String sessionId = sp.getString("sessionId", "");

                Type responseType = new TypeToken<BaseResponse<PagedResult<Backpack>>>(){}.getType();
                BaseResponse<PagedResult<Backpack>> response = NetworkUtil.getWithSession(
                        NetworkUtil.baseURL + "user/getBackpackList",
                        sessionId,
                        responseType
                );

                requireActivity().runOnUiThread(() -> {
                    if (response == null || response.getCode() != 0) {
                        binding.emptyView.setVisibility(View.VISIBLE);
                        return;
                    }
                    List<Backpack> items = response.getResult().getItems();
                    updateBackpackList(items);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    binding.emptyView.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "同步失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void updateBackpackList(List<Backpack> items) {
        if (items == null || items.isEmpty()) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.backpackRecyclerview.setVisibility(View.GONE);
            return;
        }

        BackpackItemAdapter adapter = new BackpackItemAdapter(items, this);
        binding.backpackRecyclerview.setAdapter(adapter);
        binding.emptyView.setVisibility(View.GONE);
    }

    private void activateBackpack(String backpackName) {
        new Thread(() -> {
            try {
                SharedPreferences sp = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
                String sessionId = sp.getString("sessionId", "");
                String backpackId = String.valueOf(new Random().nextInt(1000000));

                JSONObject json = new JSONObject();

                String url = NetworkUtil.baseURL + "backpack/activate?"
                        + "backpackId=" + backpackId
                        + "&backpackName=" + backpackName
                        + "&backpackBattery=100"
                        + "&backpackIpAddress=" + "19.2.2.2"
                        + "&backpackMacAddress=" + "192.168.1.1"
                        + "&backpackLocation=" +"1.2.12";

                String response = NetworkUtil.post(url, json.toString(),sessionId);
                // 解析激活响应
                Type responseType = new TypeToken<BaseResponse<Backpack>>(){}.getType();
                BaseResponse<Backpack> res = NetworkUtil.parseResponse(response, responseType);
                Log.d("增加背包","响应"+new Gson().toJson(response));

                requireActivity().runOnUiThread(() -> {
                    if (res.getCode() == 0) {
                        Toast.makeText(requireContext(), "激活成功", Toast.LENGTH_SHORT).show();
                        syncBackpack();
                    } else {
                        Toast.makeText(requireContext(), "错误: " + res.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (UnsupportedEncodingException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "编码错误", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "激活失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
                Log.d("error",e.getMessage());
            }
        }).start();
    }

}
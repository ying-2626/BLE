package com.example.backpackapplication.ui.dashboard;

import android.bluetooth.BluetoothDevice;
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
import com.example.backpackapplication.util.BLEMessageReceiver;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;
import java.util.UUID;

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
        // 可在Adapter中显示uuid
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

    // 新增：根据UUID选择背包
    public Backpack findBackpackByUuid(String uuidStr) {
        if (uuidStr == null) return null;
        List<Backpack> items = getCurrentBackpackList();
        for (Backpack bp : items) {
            if (uuidStr.equalsIgnoreCase(bp.getBackpackId())) {
                return bp;
            }
        }
        return null;
    }


    // 获取当前背包列表（已加载到Adapter）
    private List<Backpack> getCurrentBackpackList() {
        BackpackItemAdapter adapter = (BackpackItemAdapter) binding.backpackRecyclerview.getAdapter();
        return adapter != null ? adapter.getData() : java.util.Collections.emptyList();
    }

    // 供BluetoothMessageReceiver回调调用
    public void onUserConfirmedBluetoothOperation(BluetoothDevice device, String backpackId, String message) {
        Backpack bp = findBackpackByUuid(backpackId);
        if (bp == null) {
            Toast.makeText(requireContext(), "未找到对应UUID的背包", Toast.LENGTH_SHORT).show();
            return;
        }

        // 解析消息，格式如 "rfidTagId:cmd[:itemName]"
        String[] parts = message.split(":");
        if (parts.length < 2) {
            Toast.makeText(requireContext(), "蓝牙消息格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        String rfidTagId = parts[0];
        int cmd;
        try {
            cmd = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "指令编号格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        String itemName = parts.length > 2 ? parts[2] : "";

        switch (cmd) {
            case 0: // 激活标签并增加物品（首次放入包，创建标签并标记在包内）
                activateTagAndAddItem(rfidTagId, itemName, bp.getBackpackId());
                break;
            case 1: // 增加物品（标签已激活，物品重新放入包，状态变为在包内）
                addItemToBackpack(rfidTagId, bp.getBackpackId());
                break;
            case 2: // 修改物品名称
                updateTagName(rfidTagId, itemName);
                break;
            case 3: // 删除标签并移除物品（彻底删除标签，激活列表和包内都不显示）
            case 5: // 删除标签
                deleteTag(rfidTagId);
                break;
            case 4: // 移除物品（标签保留在激活标签列表，但不显示在包内物品列表）
                removeItemFromBackpack(rfidTagId, bp.getBackpackId());
                break;
            default:
                Toast.makeText(requireContext(), "未知指令编号: " + cmd, Toast.LENGTH_SHORT).show();
        }
    }

    // 激活标签并增加物品（首次放入包，POST）
    private void activateTagAndAddItem(String rfidTagId, String itemName, String backpackId) {
        new Thread(() -> {
            try {
                String sessionId = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                        .getString("sessionId", "");
                org.json.JSONObject json = new org.json.JSONObject();
                json.put("rfidTagId", rfidTagId);
                json.put("itemName", itemName);
                json.put("backpackId", backpackId);

                java.util.Map<String, Object> response = com.example.backpackapplication.util.NetworkUtil.postWithResponseCode(
                        com.example.backpackapplication.util.NetworkUtil.baseURL + "rfidTag/activateAndAdd",
                        json.toString(),
                        sessionId
                );
                requireActivity().runOnUiThread(() -> {
                    if ((int)response.get("code") == 200 || (int)response.get("code") == 0) {
                        Toast.makeText(requireContext(), "激活并添加物品成功", Toast.LENGTH_SHORT).show();
                        syncBackpack();
                    } else {
                        Toast.makeText(requireContext(), "激活并添加失败: " + response.get("message"), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "激活并添加异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // 增加物品（标签已激活，物品重新放入包，POST，状态改为在包内）
    private void addItemToBackpack(String rfidTagId, String backpackId) {
        new Thread(() -> {
            try {
                String sessionId = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                        .getString("sessionId", "");
                org.json.JSONObject json = new org.json.JSONObject();
                json.put("rfidTagId", rfidTagId);
                json.put("backpackId", backpackId);
                json.put("rfidTagStatus", 1); // 1=在包内

                java.util.Map<String, Object> response = com.example.backpackapplication.util.NetworkUtil.postWithResponseCode(
                        com.example.backpackapplication.util.NetworkUtil.baseURL + "rfidTag/addItem",
                        json.toString(),
                        sessionId
                );
                requireActivity().runOnUiThread(() -> {
                    if ((int)response.get("code") == 200 || (int)response.get("code") == 0) {
                        Toast.makeText(requireContext(), "物品已放入背包", Toast.LENGTH_SHORT).show();
                        syncBackpack();
                    } else {
                        Toast.makeText(requireContext(), "增加物品失败: " + response.get("message"), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "增加物品异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // 修改物品名称（POST）
    private void updateTagName(String rfidTagId, String newName) {
        new Thread(() -> {
            try {
                String sessionId = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                        .getString("sessionId", "");
                org.json.JSONObject json = new org.json.JSONObject();
                json.put("rfidTagId", rfidTagId);
                json.put("itemName", newName);

                java.util.Map<String, Object> response = com.example.backpackapplication.util.NetworkUtil.postWithResponseCode(
                        com.example.backpackapplication.util.NetworkUtil.baseURL + "rfidTag/updateRfidTag",
                        json.toString(),
                        sessionId
                );
                requireActivity().runOnUiThread(() -> {
                    if ((int)response.get("code") == 200 || (int)response.get("code") == 0) {
                        Toast.makeText(requireContext(), "修改物品名称成功", Toast.LENGTH_SHORT).show();
                        syncBackpack();
                    } else {
                        Toast.makeText(requireContext(), "修改物品名称失败: " + response.get("message"), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "修改物品名称异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // 删除标签（并移除物品，GET，彻底删除）
    private void deleteTag(String rfidTagId) {
        new Thread(() -> {
            try {
                String sessionId = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                        .getString("sessionId", "");
                com.example.backpackapplication.util.BaseResponse<?> response =
                        com.example.backpackapplication.util.NetworkUtil.getWithSession(
                                com.example.backpackapplication.util.NetworkUtil.baseURL +
                                        "rfidTag/deleteRfidTag?rfidTagId=" + rfidTagId,
                                sessionId,
                                new com.google.gson.reflect.TypeToken<com.example.backpackapplication.util.BaseResponse<Void>>(){}.getType()
                        );
                requireActivity().runOnUiThread(() -> {
                    if (response.getCode() == 0) {
                        Toast.makeText(requireContext(), "标签已删除", Toast.LENGTH_SHORT).show();
                        syncBackpack();
                    } else {
                        Toast.makeText(requireContext(), "删除标签失败: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "删除标签异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // 移除物品（不删除标签，仅状态变更，GET，状态改为不在包内）
    private void removeItemFromBackpack(String rfidTagId, String backpackId) {
        new Thread(() -> {
            try {
                String sessionId = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                        .getString("sessionId", "");
                com.example.backpackapplication.util.BaseResponse<?> response =
                        com.example.backpackapplication.util.NetworkUtil.getWithSession(
                                com.example.backpackapplication.util.NetworkUtil.baseURL +
                                        "rfidTag/removeItem?rfidTagId=" + rfidTagId +
                                        "&backpackId=" + backpackId,
                                sessionId,
                                new com.google.gson.reflect.TypeToken<com.example.backpackapplication.util.BaseResponse<Void>>(){}.getType()
                        );
                requireActivity().runOnUiThread(() -> {
                    if (response.getCode() == 0) {
                        Toast.makeText(requireContext(), "物品已移除", Toast.LENGTH_SHORT).show();
                        syncBackpack();
                    } else {
                        Toast.makeText(requireContext(), "移除物品失败: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "移除物品异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

}

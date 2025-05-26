package com.example.backpackapplication.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.backpackapplication.LoginActivity;
import com.example.backpackapplication.R;
import com.example.backpackapplication.databinding.DialogModifyInfoBinding;
import com.example.backpackapplication.databinding.DialogModifyPasswordBinding;
import com.example.backpackapplication.databinding.FragmentHomeBinding;
import com.example.backpackapplication.util.BaseResponse;
import com.example.backpackapplication.util.NetworkUtil;
import com.example.backpackapplication.util.model.RfidTag;
import com.example.backpackapplication.util.model.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private User currentUser;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // 初始化按钮监听
        binding.modifyInfoButton.setOnClickListener(v -> showModifyInfoDialog());
        binding.modifyPasswordButton.setOnClickListener(v -> showModifyPasswordDialog());
        binding.exitLoginButton.setOnClickListener(v -> onDelete());

        loadUserInfo();
        return binding.getRoot();
    }



    private void loadUserInfo() {
        SharedPreferences sp = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String sessionId = sp.getString("sessionId", "");

        new Thread(() -> {
            try {
                Type responseType = new TypeToken<BaseResponse<User>>(){}.getType();
                BaseResponse<User> response = NetworkUtil.getWithSession(
                        NetworkUtil.baseURL + "user/getUserInfo",
                        sessionId,
                        responseType
                );

                requireActivity().runOnUiThread(() -> {
                    if (response.getCode() == 0) {
                        currentUser = response.getResult();
                        updateUserUI();
                    }else {
                        Toast.makeText(requireContext(),
                                "服务端错误("+response.getCode()+")："+response.getMessage(),
                                Toast.LENGTH_LONG).show();
                        exitLogin();
                    }
                });
            } catch (Exception e) {
               requireActivity().runOnUiThread(() ->
                showToast("加载失败: " + e.getMessage()));
            }
        }).start();
    }

    private void updateUserUI() {
        binding.usernameValueText.setText(currentUser.getUsername());
        binding.contactInfoTextView.setText(currentUser.getContactInfo());
    }


    private void exitLogin() {
        // 清空本地session
        clearSession();
        // 跳转到登录页并清除返回栈
        navigateToLogin();

        // 关闭当前Activity
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
    private void clearSession() {
        SharedPreferences sp = requireContext().getSharedPreferences(
                "user_session",
                Context.MODE_PRIVATE
        );
        sp.edit()
                .remove("sessionId")
                .apply(); // 使用apply()异步提交
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);

        // 清除任务栈（按需选择其中一个方案）

        // 方案1：完全重新启动应用
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 方案2：仅清除当前Activity栈
        // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    public void onDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle("确认")
                .setMessage("用户"+currentUser.getUsername()+"，是否确认退出登录")
                .setPositiveButton("确定", (d, w) -> exitLogin())
                .setNegativeButton("取消", null)
                .show();
    }

    private void showModifyInfoDialog() {
        // 使用视图绑定初始化对话框
        DialogModifyInfoBinding dialogBinding = DialogModifyInfoBinding.inflate(getLayoutInflater());

        // 填充当前数据
        dialogBinding.usernameEdittext.setText(currentUser.getUsername());
        dialogBinding.contactInfoEdittext.setText(currentUser.getContactInfo());

        new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .setPositiveButton("保存", (d, which) -> {
                    String newUsername = dialogBinding.usernameEdittext.getText().toString().trim();
                    String newContactInfo = dialogBinding.contactInfoEdittext.getText().toString().trim();

                    if (validateInput(newUsername,newContactInfo)) {
                        updateUserInfo(newUsername,newContactInfo);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showModifyPasswordDialog() {
        // 使用视图绑定初始化对话框
        DialogModifyPasswordBinding dialogBinding = DialogModifyPasswordBinding.inflate(getLayoutInflater());

        new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .setPositiveButton("保存", (d, which) -> {
                    String oldPassword = dialogBinding.oldPasswordEdittext.getText().toString().trim();
                    String newPassword = dialogBinding.newPasswordEdittext.getText().toString().trim();

                    if (validateInput(oldPassword,newPassword)) {
                      updatePassword(oldPassword,newPassword);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean validateInput(String... inputs) {
        for (String input : inputs) {
            if (TextUtils.isEmpty(input)) {
                showToast("所有字段不能为空");
                return false;
            }
        }
        return true;
    }


    // HomeFragment.java 中新增/修改的方法

    private void updateUserInfo(String newUsername, String newContact) {
        new Thread(() -> {
            try {
                // 获取会话信息
                SharedPreferences sp = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
                String sessionId = sp.getString("sessionId", "");
                // 正确构造请求参数（URL参数形式）
                String url = NetworkUtil.baseURL + "user/updateUserInfo"
                        + "?username=" +newUsername
                        + "&contactInfo=" + newContact;

                // 发送请求
                Type responseType = new TypeToken<BaseResponse<User>>(){}.getType();
                Map<String,Object> response = NetworkUtil.postWithResponseCode(
                    url, "",
                        sessionId
                );
                Log.d("info","响应体"+response);

                // 处理响应
                requireActivity().runOnUiThread(() -> {
                    if ((int)response.get("code") == 200) {
                        currentUser = (User)response.get("result");
                        loadUserInfo();
                        showToast("信息更新成功");
                    } else {
                        showToast("更新失败: " + response.get("message"));
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        showToast("请求异常: " + e.getMessage())
                );
            }
        }).start();
    }

    private void updatePassword(String oldPwd, String newPwd) {
        new Thread(() -> {
            try {
                // 获取会话信息
                SharedPreferences sp = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
                String sessionId = sp.getString("sessionId", "");

                // 发送请求
                Type responseType = new TypeToken<BaseResponse<User>>(){}.getType();
                BaseResponse<User> response = NetworkUtil.postWithSession(
                        NetworkUtil.baseURL + "user/changePassword?oldPassword="
                                +oldPwd+"&newPassword="+newPwd,
                       "",
                        sessionId,
                        responseType
                );

                // 处理响应
                requireActivity().runOnUiThread(() -> {
                    if (response.getCode() == 0) {
                        showToast("密码修改成功");
                        // 如果需要重新登录可以在这里处理
                    } else {
                        showToast("修改失败: " + response.getMessage());
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        showToast("请求异常: " + e.getMessage())
                );
            }
        }).start();
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
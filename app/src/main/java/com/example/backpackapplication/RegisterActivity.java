package com.example.backpackapplication;

import static com.example.backpackapplication.util.NetworkUtil.post;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.backpackapplication.databinding.ActivityRegisterBinding;
import com.example.backpackapplication.util.NetworkUtil;
import com.example.backpackapplication.util.SignupResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +         // 起始符
                    "(?=.*[0-9])" +       // 至少一个数字
                    "(?=.*[a-zA-Z])" +    // 任意字母
                    "(?=\\S+$)" +         // 无空格
                    ".{6,}" +            // 至少6个字符
                    "$");                // 结束符

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRegisterBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_register);


        binding.registerButton.setOnClickListener(l -> {
            // 获取输入值并去除空格
            String username = binding.usernameInput.getText().toString().trim();
            String password = binding.passwordInput.getText().toString().trim();
            String contactInfo = binding.contactInfoInput.getText().toString().trim();

            // 输入合法性检查
            if (!validateInput(username, password, contactInfo)) {
                return;
            }

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("username", username);
                jsonObject.put("password", password);
                jsonObject.put("contactInfo", contactInfo);
                //Log.d("NetworkRequest", "Sending: " + jsonObject.toString());
            } catch (JSONException e) {
                showToast("数据格式错误");
                return;
            }
            new Thread(() -> {
                try {
                    String url = NetworkUtil.baseURL + "user/signup?username="+username+"&password="+password+"&contactInfo="+contactInfo;
                    String response = NetworkUtil.post(url, jsonObject.toString());
                    // 添加空响应检查
                    if (response == null || response.isEmpty()) {
                        runOnUiThread(() -> showToast("服务器无响应"));
                        return;
                    }

                    Log.d("Network", "原始响应：" + response);
                    // 类型安全解析
                    SignupResponse res = NetworkUtil.parseResponse(response, SignupResponse.class);

                    // 状态码验证
                    if (res.getCode() != 0) {
                        runOnUiThread(() -> showToast(res.getMessage()));
                        return;
                    }

                    // 嵌套对象验证
                    if (res.getResult() == null || res.getResult().getSessionId() == null) {
                        runOnUiThread(() -> showToast("会话信息异常"));
                        return;
                    }

                    // 保存会话信息
                    saveSession(res.getResult());
                    runOnUiThread(() -> {
                        showToast("注册成功");
                        startActivity(new Intent(this, MountActivity.class));
                        finish();
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> showToast("请求异常：" + e.getMessage()));
                }
            }).start();
        });
    }





    private boolean validateInput(String username, String password, String contactInfo) {
        if (username.isEmpty() || password.isEmpty() || contactInfo.isEmpty()) {
            showToast("有信息未填写");
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            showToast("密码需至少6位且包含字母和数字");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(contactInfo).matches() &&
                !Patterns.PHONE.matcher(contactInfo).matches()) {
            showToast("请输入有效的邮箱或手机号");
            return false;
        }

        return true;
    }

    private void saveSession(SignupResponse.UserResult result) {
        if (result == null || result.getSessionId() == null) {
            showToast("会话信息异常");
            return;
        }
        getSharedPreferences("user_session", MODE_PRIVATE)
                .edit()
                .putString("sessionId", result.getSessionId())
                .putString("username", result.getUsername())
                .apply();

        Log.d("Session", "保存成功 SessionID: " + result.getSessionId());
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // RegisterActivity.java
    public void navigateToLogin(View view) {
        startActivity(new Intent(this, LoginActivity.class));
        finish(); // 可选，关闭当前注册页面
    }
}

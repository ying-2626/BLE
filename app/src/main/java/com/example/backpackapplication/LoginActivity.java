package com.example.backpackapplication;

import static com.example.backpackapplication.util.NetworkUtil.post;
import static com.example.backpackapplication.util.NetworkUtil.postWithResponseCode;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.backpackapplication.databinding.ActivityLoginBinding;
import com.example.backpackapplication.util.BaseResponse;
import com.example.backpackapplication.util.NetworkUtil;
import com.example.backpackapplication.util.SignupResponse;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        autoCheckLoginStatus();

        ActivityLoginBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        binding.loginRequestButton.setOnClickListener(l -> {
            // 获取输入值并去除空格
            String username = binding.usernameInputText.getText().toString().trim();
            String password = binding.userPasswordInputText.getText().toString().trim();

            new Thread(()->{
               JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("username", username);
                    jsonObject.put("password", password);
                    Log.d("NetworkRequest", "Sending: " + jsonObject.toString());
                } catch (JSONException e) {
                    showToast("数据格式错误");
                    return;
                }
                // 发送请求
                try {
                    String url = NetworkUtil.baseURL + "user/login?username="+username+"&password="+password;

                    Map<String,Object> responseMap = postWithResponseCode(url, jsonObject.toString());

                    int httpCode = (Integer) responseMap.get("code");
                    if (httpCode != 200) {
                        runOnUiThread(() -> showToast("服务异常: " + httpCode));
                        return;
                    }

// 正确获取响应体
                    String responseBody = (String) responseMap.get("body");

// 空响应检查
                    if (responseBody == null || responseBody.isEmpty()) {
                        runOnUiThread(() -> showToast("服务器无响应"));
                        return;
                    }

// 使用正确类型解析
                    SignupResponse res = NetworkUtil.parseResponse(responseBody, SignupResponse.class);


                    // 业务码验证
                    if (res.getCode() != 0) {
                        runOnUiThread(() -> showToast(res.getMessage()));
                        return;
                    }

                    //会话信息验证
                    if (res.getResult() == null || res.getResult().getSessionId() == null) {
                        runOnUiThread(() -> showToast("会话信息异常"));
                        return;
                    }

                    // 保存会话信息
                    saveSession(res.getResult());
                    runOnUiThread(() -> {
                        showToast("登录成功");
                        startActivity(new Intent(this, MountActivity.class));
                        finish();
                    });

                } catch (JSONException e) {
                    Log.e("Login", "JSON构建失败", e);
                    runOnUiThread(() -> showToast("输入格式错误"));
                } catch (Exception e) {
                    Log.e("Login", "网络请求异常", e);
                    runOnUiThread(() -> showToast("登录失败：" + e.getMessage()));
                }
            }).start();
        });

        binding.turnToRegisterbutton.setOnClickListener(l ->{
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void showLoginUI() {
        // 显示登录界面元素
        findViewById(R.id.main).setVisibility(View.VISIBLE);
    }

    private void autoCheckLoginStatus() {
        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                String sessionId = prefs.getString("sessionId", null);

                if (TextUtils.isEmpty(sessionId)) {
                    runOnUiThread(() -> showLoginUI());
                    return;
                }

                // 使用新API检查会话
                Type responseType = new TypeToken<BaseResponse<Integer>>(){}.getType();
                BaseResponse<Integer> response = NetworkUtil.getWithSession(
                        NetworkUtil.baseURL + "user/checkLogin",
                        sessionId,
                        responseType
                );
if(response.getCode()==0)
{
    Intent intent = new Intent(LoginActivity.this, MountActivity.class);
    startActivity(intent);
}else {
    runOnUiThread(() -> {
        clearSession();
        showLoginUI();
    });
}
            } catch (Exception e) {
                runOnUiThread(() -> showToast("自动登录检查失败"));
            }
        }).start();
    }



    private void clearSession() {
        getSharedPreferences("user_session", MODE_PRIVATE)
                .edit()
                .remove("sessionId")
                .apply();
    }

    private void saveSession(SignupResponse.UserResult result) {
        Log.d("Session", "保存Session: " + result.getSessionId());
        getSharedPreferences("user_session", MODE_PRIVATE)
                .edit()
                .putString("sessionId", result.getSessionId())
                .putString("username", result.getUsername())
                .apply();
    }
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}


package com.example.backpackapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.backpackapplication.adapter.ActionHistoryAdapter;
import com.example.backpackapplication.databinding.ActivityTagActionBinding;
import com.example.backpackapplication.util.BaseResponse;
import com.example.backpackapplication.util.NetworkUtil;
import com.example.backpackapplication.util.PagedResult;
import com.example.backpackapplication.util.model.RfidTagAction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.List;

// TagActionActivity.java
public class TagActionActivity extends AppCompatActivity {

    private ActivityTagActionBinding binding;
    private ActionHistoryAdapter adapter;
    private String rfidTagId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTagActionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 获取传递的RFID标签ID
        rfidTagId = getIntent().getStringExtra("rfidTagId");

        initViews();
        loadActionHistory();
    }

    private void initViews() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 隐藏默认标题，显示自定义 TextView
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        adapter = new ActionHistoryAdapter();
        binding.actionRecyclerView.setAdapter(adapter);
    }

    private void loadActionHistory() {
        showLoading(true);

        new Thread(() -> {
            try {
                SharedPreferences sp = getSharedPreferences("user_session", MODE_PRIVATE);
                String sessionId = sp.getString("sessionId", "");

                String url = NetworkUtil.baseURL + "rfidTagAction/findRfidTagActionByRfidTagId"
                        + "?rfidTagId=" + rfidTagId
                        +"&orderByCreateTime="+true
                        +"&page="+1
                        +"&pageSize="+50;

                Type responseType = new TypeToken<BaseResponse<PagedResult<RfidTagAction>>>(){}.getType();
                BaseResponse<PagedResult<RfidTagAction>> response = NetworkUtil.getWithSession(
                        url, sessionId, responseType
                );
                Log.d("TagActionData", "完整响应: " + new Gson().toJson(response));

                runOnUiThread(() -> {
                    showLoading(false);

                    if (response.getCode() == 0 && response.getResult() != null) {
                        List<RfidTagAction> items = response.getResult().getItems();
                        Log.d("TagAction", "加载到数据数量: " + (items != null ? items.size() : 0));
                        handleData(items);
                    } else {
                        String errorMsg = response.getMessage() != null ? response.getMessage() : "未知错误";
                        Log.e("TagAction", "加载失败: " + errorMsg);
                        showError(errorMsg);
                    }
                });
            } catch (Exception e) {
                Log.e("TagAction", "请求异常: ", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(e.getMessage());
                });
            }
        }).start();
    }

    private void handleData(List<RfidTagAction> actions) {
        if (actions == null || actions.isEmpty()) {
            Log.d("TagAction", "数据为空，显示空视图");
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.actionRecyclerView.setVisibility(View.GONE); // 隐藏 RecyclerView
        } else {
            Log.d("TagAction", "更新数据，数量: " + actions.size());
            binding.emptyView.setVisibility(View.GONE);
            binding.actionRecyclerView.setVisibility(View.VISIBLE); // 显示 RecyclerView
            adapter.updateData(actions);
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.actionRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.emptyView.setVisibility(View.GONE); // 加载时隐藏空视图
    }

    private void showError(String message) {
        Toast.makeText(this, "加载失败: " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
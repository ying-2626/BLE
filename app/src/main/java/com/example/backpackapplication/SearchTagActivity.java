package com.example.backpackapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.backpackapplication.adapter.TagItemAdapter;
import com.example.backpackapplication.databinding.ActivitySearchTagBinding;
import com.example.backpackapplication.util.BaseResponse;
import com.example.backpackapplication.util.NetworkUtil;
import com.example.backpackapplication.util.PagedResult;
import com.example.backpackapplication.util.UserUtil;
import com.example.backpackapplication.util.model.RfidTag;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static android.content.Context.MODE_PRIVATE;

public class SearchTagActivity extends AppCompatActivity implements TagItemAdapter.OnTagActionListener{
    private ActivitySearchTagBinding binding;
    private TagItemAdapter adapter;
    private String currentBackpackId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new TagItemAdapter(this,this);

        // 获取传递的backpackId
        currentBackpackId = getIntent().getStringExtra("BACKPACK_ID");
        binding = DataBindingUtil.setContentView(this, R.layout.activity_search_tag);

        // 设置 Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 显示返回按钮
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setupRecyclerView();
        setupEventListeners();
        syncTags();
    }

    private void setupRecyclerView() {
        binding.tagRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.tagRecyclerView.setAdapter(adapter);
    }

    private void setupEventListeners() {
        binding.addTagButton.setOnClickListener(v -> showAddTagDialog());
    }

    // 标签同步（带加载状态）
    private void syncTags() {

        runOnUiThread(() -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tagRecyclerView.setVisibility(View.GONE);
            binding.emptyView.setVisibility(View.GONE);
        });

        new Thread(() -> {
            try {
                SharedPreferences sp = getSharedPreferences("user_session", MODE_PRIVATE);
                String sessionId = sp.getString("sessionId", "");

                // 构建带分页参数的请求
                String url = NetworkUtil.baseURL + "backpack/queryRfidTags?backpackId=" +
                        currentBackpackId;

                // 使用泛型解析响应
                Type responseType = new TypeToken<BaseResponse<PagedResult<RfidTag>>>(){}.getType();
                BaseResponse<PagedResult<RfidTag>> response = NetworkUtil.getWithSession(
                        url, sessionId, responseType
                );
                Log.d("BackpackData", "完整响应: " + new Gson().toJson(response));

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.getCode() == 0 && response.getResult().getItems()  !=null) {
                        handleTagData(response.getResult().getItems());
                    } else {
                        binding.emptyView.setVisibility(View.VISIBLE);
                        showError("同步失败：" + response.getMessage());
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    showError("网络异常：" + e.getMessage());
                });
            }finally {
                runOnUiThread(() -> binding.progressBar.setVisibility(View.GONE));
            }
        }).start();
    }

    private void handleTagData(List<RfidTag> tags) {

        if (tags==null||tags.isEmpty()) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.tagRecyclerView.setVisibility(View.GONE);
            return;
        }
        else{
        adapter.updateData(tags);
        binding.emptyView.setVisibility(View.GONE);
        binding.tagRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    // 新增标签对话框（带输入校验）
    private void showAddTagDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("添加新标签")
                .setView(R.layout.tag_dialog)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            EditText etTagName = dialog.findViewById(R.id.tagNameInput);
            Button btnConfirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnConfirm.setOnClickListener(v -> {
                String tagName = etTagName.getText().toString().trim();

                if (validateInput( tagName)) {

                    createTag(tagName);
                    dialog.dismiss();
                }
            });
        });
        dialog.show();
    }

    private boolean validateInput(String tagName) {
        if (tagName.isEmpty()) {
            showError("标签名称不能为空");
            return false;
        }
        return true;
    }

    // 标签创建（带重试机制）
    private void createTag(String tagName) {

        SharedPreferences sp = getSharedPreferences("user_session", MODE_PRIVATE);
        String sessionId = sp.getString("sessionId", "");

        int randomId = new Random().nextInt(1000000);
        String rfidTagId = String.valueOf(randomId);

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();

                Map<String, Object> response = NetworkUtil.postWithResponseCode(
                        NetworkUtil.baseURL + "rfidTag/addRfidTag?rfidTagId="+rfidTagId
                                +"&itemName="+tagName
                        +"&backpackId="+currentBackpackId,
                        json.toString(),
                        sessionId
                );
                Log.d("BackpackData", "完整响应: " + new Gson().toJson(response));
                runOnUiThread(() -> {
                    if ((int)response.get("code") == 200) {
                        syncTags();
                        Toast.makeText(this, "添加标签成功", Toast.LENGTH_SHORT).show();
                    } else {
                        showError("创建失败：" + response.get("message"));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("创建异常：" + e.getMessage()));
            }
        }).start();
    }

    // 标签删除（带二次确认）

    public void onDelete(RfidTag tag) {
     /*   new AlertDialog.Builder(this)
                .setTitle("删除确认")
                .setMessage("确定删除标签 " + tag.getItemName() + "？")
                .setPositiveButton("删除", (d, w) -> deleteTag(tag))
                .setNegativeButton("取消", null)
                .show();
      */
        deleteTag(tag);
    }

    @Override
    public void onEdit(RfidTag tag) {

    }

    private void deleteTag(RfidTag tag) {
       String sessionId=getSessionId();
        new Thread(() -> {
            try {

                BaseResponse<?> response = NetworkUtil.getWithSession(
                        NetworkUtil.baseURL + "rfidTag/deleteRfidTag?rfidTagId="+tag.getRfidTagId(),
                      sessionId,
                        new TypeToken<BaseResponse<Void>>(){}.getType()
                );

                runOnUiThread(() -> {
                    if (response.getCode() ==0) {
                        syncTags();
                        Toast.makeText(this,"删除成功", Toast.LENGTH_SHORT).show();
                    } else {
                        showError("删除失败：" + response.getMessage());
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("删除异常：" + e.getMessage()));
            }
        }).start();
    }

    // 工具方法
    private String getSessionId() {
        return getSharedPreferences("user_session", MODE_PRIVATE)
                .getString("sessionId", "");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // 处理返回按钮点击
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
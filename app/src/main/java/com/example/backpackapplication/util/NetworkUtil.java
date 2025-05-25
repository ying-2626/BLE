package com.example.backpackapplication.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetworkUtil {
    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    private static OkHttpClient client = new OkHttpClient();
    public static final String baseURL = "http://124.222.125.175:8080/";
    private static final Gson gson = new Gson();
    private static final Type objectType = new TypeToken<Map<String, Object>>(){}.getType();
    private static final Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();

    public static String post(String url, String json) throws Exception {
        return post(url, json, null);
    }

    // 重载带session的POST方法
    public static String post(String url, String json, String sessionId) throws Exception {
        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json");

        if (sessionId != null && !sessionId.isEmpty()) {
            builder.addHeader( "session", sessionId);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            return response.body().string();
        }
    }


    // 原始带响应码的方法
    public static Map<String,Object> postWithResponseCode(String url, String json) throws Exception {
        return postWithResponseCode(url, json, null);
    }

    // 重载带session的响应码方法
    public static Map<String,Object> postWithResponseCode(String url, String json, String sessionId) throws Exception {
        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);

        if (sessionId != null && !sessionId.isEmpty()) {
            builder.addHeader("session" , sessionId);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            Map<String, Object> result = new HashMap<>();
            result.put("body", response.body().string());
            result.put("code", response.code());
            return result;
        }
    }

        // 新增带泛型的POST方法
        public static <T> BaseResponse<T> postWithSession(String url,
                                                          String jsonBody,
                                                          String sessionId,
                                                          Type responseType) throws IOException {
            RequestBody body = RequestBody.create(jsonBody, JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("session", sessionId)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                return new Gson().fromJson(responseBody, responseType);
            }
        }



    public static <T> T getWithSession(String url, String sessionId, Type type) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("session" , sessionId)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // 输出错误详情
                String errorBody = response.body() != null ? response.body().string() : "无响应内容";
                Log.e("NetworkUtil", "HTTP 错误码: " + response.code() + ", 错误信息: " + errorBody);
                throw new IOException("HTTP " + response.code() + ": " + errorBody);
            }
            String responseBody = response.body().string();
            return new Gson().fromJson(responseBody, type);
        }
    }

    public static <T> T parseResponse(String json, Class<T> type) {
        return new Gson().fromJson(json, type);
    }

    public static <T> BaseResponse<T> parseResponse(String json, Type type) {
        return new Gson().fromJson(json, type);
    }
}


package com.example.backpackapplication.util;

public class BaseResponse<T> {
    private int code;
    private String message;
    private T result;

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getResult() { return result; }
}
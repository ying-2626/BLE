// PagedResult.java
package com.example.backpackapplication.util;

import java.util.List;

public class PagedResult<T> {
    private int pageNum;
    private int pageSize;
    private int total;
    private int pages;
    private List<T> items;

    public List<T> getItems() { return items; }
    // ...其他getters
}
package com.example.demo.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * セッションに格納する画面遷移管理Bean
 */
public class NavigationSession implements Serializable {

    private final List<PageVisit> history = new ArrayList<>();
    private PageVisit currentPage;

    public void addVisit(String screenCode) {
        currentPage = new PageVisit(screenCode);
        history.add(currentPage);
    }

    public PageVisit getCurrentPage() {
        return currentPage;
    }

    public List<PageVisit> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public int size() {
        return history.size();
    }
}

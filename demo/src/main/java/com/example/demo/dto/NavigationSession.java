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

    public void addVisit(String screenCode) {
        history.add(new PageVisit(screenCode));
    }

    public List<PageVisit> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public int size() {
        return history.size();
    }
}

package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public NavigationSession() {}

    @JsonCreator
    public NavigationSession(
            @JsonProperty("history") List<PageVisit> history,
            @JsonProperty("currentPage") PageVisit currentPage) {
        if (history != null) this.history.addAll(history);
        this.currentPage = currentPage;
    }

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

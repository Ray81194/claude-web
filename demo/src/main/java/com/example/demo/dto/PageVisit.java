package com.example.demo.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 画面遷移情報DTO
 */
public class PageVisit implements Serializable {

    private String screenCode;
    private LocalDateTime visitedAt;

    public PageVisit() {}

    public PageVisit(String screenCode) {
        this.screenCode = screenCode;
        this.visitedAt = LocalDateTime.now();
    }

    public String getScreenCode() { return screenCode; }
    public void setScreenCode(String screenCode) { this.screenCode = screenCode; }

    public LocalDateTime getVisitedAt() { return visitedAt; }
    public void setVisitedAt(LocalDateTime visitedAt) { this.visitedAt = visitedAt; }

    @Override
    public String toString() {
        return screenCode + " @ " + visitedAt;
    }
}

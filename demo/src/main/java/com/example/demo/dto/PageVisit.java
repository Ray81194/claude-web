package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 画面遷移情報DTO
 */
public class PageVisit implements Serializable {

    private final String screenCode;
    private final LocalDateTime visitedAt;

    public PageVisit(String screenCode) {
        this.screenCode = screenCode;
        this.visitedAt = LocalDateTime.now();
    }

    @JsonCreator
    public PageVisit(
            @JsonProperty("screenCode") String screenCode,
            @JsonProperty("visitedAt") LocalDateTime visitedAt) {
        this.screenCode = screenCode;
        this.visitedAt = visitedAt;
    }

    public String getScreenCode() { return screenCode; }

    public LocalDateTime getVisitedAt() { return visitedAt; }

    @Override
    public String toString() {
        return screenCode + " @ " + visitedAt;
    }
}

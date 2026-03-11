package com.example.demo.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageVisitSerdeTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void currentPage_シリアライズしてデシリアライズできること() throws Exception {
        // NavigationSession を作り HOME を訪問
        NavigationSession session = new NavigationSession();
        session.addVisit("HOME");

        // NavigationSession ごと JSON にシリアライズ
        String json = mapper.writeValueAsString(session);
        System.out.println("JSON: " + json);

        // currentPage フィールドが JSON に含まれること
        assertThat(json).contains("currentPage");
        assertThat(json).contains("screenCode");
        assertThat(json).contains("HOME");

        // JSON から NavigationSession をデシリアライズ
        NavigationSession restored = mapper.readValue(json, NavigationSession.class);

        // currentPage が正しく復元されること
        assertThat(restored.getCurrentPage()).isNotNull();
        assertThat(restored.getCurrentPage().getScreenCode()).isEqualTo("HOME");
        assertThat(restored.getCurrentPage().getVisitedAt()).isNotNull();
    }

    @Test
    void currentPage_複数遷移後も最新ページが復元できること() throws Exception {
        NavigationSession session = new NavigationSession();
        session.addVisit("HOME");
        session.addVisit("REDIS_SESSION");

        String json = mapper.writeValueAsString(session);
        System.out.println("JSON: " + json);

        NavigationSession restored = mapper.readValue(json, NavigationSession.class);

        assertThat(restored.getCurrentPage().getScreenCode()).isEqualTo("REDIS_SESSION");
    }
}

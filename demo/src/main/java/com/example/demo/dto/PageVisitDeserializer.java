package com.example.demo.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDateTime;

public class PageVisitDeserializer extends JsonDeserializer<PageVisit> {

    @Override
    public PageVisit deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        PageVisit pv = new PageVisit();
        pv.setScreenCode(node.get("screenCode").asText());
        JsonNode visitedAt = node.get("visitedAt");
        if (visitedAt != null && !visitedAt.isNull()) {
            pv.setVisitedAt(LocalDateTime.parse(visitedAt.asText()));
        }
        return pv;
    }
}

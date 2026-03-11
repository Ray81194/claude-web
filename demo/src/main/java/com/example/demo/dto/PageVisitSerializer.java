package com.example.demo.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class PageVisitSerializer extends JsonSerializer<PageVisit> {

    @Override
    public void serialize(PageVisit value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("screenCode", value.getScreenCode());
        gen.writeStringField("visitedAt", value.getVisitedAt() != null ? value.getVisitedAt().toString() : null);
        gen.writeEndObject();
    }
}

package com.example.redisdemo.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

public class OrderStatus {

    public static final OrderStatus PENDING   = new OrderStatus("01", "保留");
    public static final OrderStatus CONFIRMED = new OrderStatus("02", "確定");

    private static final Map<String, OrderStatus> CODE_MAP = Map.of(
            "01", PENDING,
            "02", CONFIRMED
    );

    private final String code;
    private final String label;

    private OrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode()  { return code; }
    public String getLabel() { return label; }

    public static OrderStatus of(String code) {
        OrderStatus status = CODE_MAP.get(code);
        if (status == null) throw new IllegalArgumentException("Unknown code: " + code);
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return code.equals(((OrderStatus) o).code);
    }

    @Override
    public int hashCode() { return code.hashCode(); }

    @Override
    public String toString() { return code + ":" + label; }

    static class Serializer extends JsonSerializer<OrderStatus> {
        @Override
        public void serialize(OrderStatus value, JsonGenerator gen,
                              SerializerProvider provider) throws IOException {
            gen.writeString(value.getCode());
        }
    }

    static class Deserializer extends JsonDeserializer<OrderStatus> {
        @Override
        public OrderStatus deserialize(JsonParser p,
                                       DeserializationContext ctxt) throws IOException {
            return OrderStatus.of(p.getValueAsString());
        }
    }
}

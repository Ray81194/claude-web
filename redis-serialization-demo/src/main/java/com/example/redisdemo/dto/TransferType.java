package com.example.redisdemo.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;
import java.util.Map;

public class TransferType {

    public static final TransferType NORMAL  = new TransferType("01", "通常");
    public static final TransferType EXPRESS = new TransferType("02", "速達");

    private static final Map<String, TransferType> CODE_MAP = Map.of(
            "01", NORMAL,
            "02", EXPRESS
    );

    private final String code;
    private final String label;

    private TransferType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode()  { return code; }
    public String getLabel() { return label; }

    public static TransferType of(String code) {
        TransferType type = CODE_MAP.get(code);
        if (type == null) throw new IllegalArgumentException("Unknown code: " + code);
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return code.equals(((TransferType) o).code);
    }

    @Override
    public int hashCode() { return code.hashCode(); }

    @Override
    public String toString() { return code + ":" + label; }

    static class Serializer extends JsonSerializer<TransferType> {
        @Override
        public void serialize(TransferType value, JsonGenerator gen,
                              SerializerProvider provider) throws IOException {
            gen.writeString(value.getCode());
        }

        @Override
        public void serializeWithType(TransferType value, JsonGenerator gen,
                                      SerializerProvider provider,
                                      TypeSerializer typeSer) throws IOException {
            serialize(value, gen, provider);
        }
    }

    static class Deserializer extends JsonDeserializer<TransferType> {
        @Override
        public TransferType deserialize(JsonParser p,
                                        DeserializationContext ctxt) throws IOException {
            return TransferType.of(p.getValueAsString());
        }
    }
}

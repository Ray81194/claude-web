package com.example.redisdemo;

import com.example.redisdemo.dto.AccountId;
import com.example.redisdemo.dto.Money;
import com.example.redisdemo.dto.OrderStatus;
import com.example.redisdemo.dto.TransferDto;
import com.example.redisdemo.dto.TransferType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

public class DefaultSerializerOrderStatusTest {

    @Test
    void serializeWithDefaultSerializer() throws Exception {
        GenericJackson2JsonRedisSerializer s = new GenericJackson2JsonRedisSerializer();

        TransferDto dto = new TransferDto();
        dto.setFromAccount(AccountId.of("AC-000001"));
        dto.setToAccount(AccountId.of("AC-000002"));
        dto.setAmount(Money.of(3000, "JPY"));
        dto.setStatus(OrderStatus.PENDING);
        dto.setTransferType(TransferType.EXPRESS);

        byte[] bytes = s.serialize(dto);
        String rawJson = new String(bytes);

        ObjectMapper mapper = new ObjectMapper();
        System.out.println("=== デフォルトシリアライザ JSON ===");
        System.out.println(mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(mapper.readValue(rawJson, Object.class)));

        System.out.println("=== デシリアライズ ===");
        try {
            TransferDto retrieved = (TransferDto) s.deserialize(bytes);
            System.out.println("status       : " + retrieved.getStatus().getCode()
                    + " / " + retrieved.getStatus().getLabel());
            System.out.println("transferType : " + retrieved.getTransferType().getCode()
                    + " / " + retrieved.getTransferType().getLabel());
        } catch (Exception e) {
            System.out.println("失敗: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}

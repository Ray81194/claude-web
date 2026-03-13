package com.example.redisdemo;

import com.example.redisdemo.dto.AccountId;
import com.example.redisdemo.dto.Money;
import com.example.redisdemo.dto.OrderStatus;
import com.example.redisdemo.dto.TransferDto;
import com.example.redisdemo.dto.TransferType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class DefaultSerializerOrderStatusTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY = "transfer:test";

    @BeforeEach
    void setUp() {
        redisTemplate.delete(KEY);
    }

    @Test
    void serializeWithDefaultSerializer() throws Exception {
        TransferDto dto = new TransferDto();
        dto.setFromAccount(AccountId.of("AC-000001"));
        dto.setToAccount(AccountId.of("AC-000002"));
        dto.setAmount(Money.of(3000, "JPY"));
        dto.setStatus(OrderStatus.PENDING);
        dto.setTransferType(TransferType.EXPRESS);

        redisTemplate.opsForValue().set(KEY, dto);

        String rawJson = stringRedisTemplate.opsForValue().get(KEY);

        ObjectMapper mapper = new ObjectMapper();
        System.out.println("=== デフォルトシリアライザ JSON ===");
        System.out.println(mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(mapper.readValue(rawJson, Object.class)));

        TransferDto retrieved = (TransferDto) redisTemplate.opsForValue().get(KEY);
        System.out.println("=== デシリアライズ ===");
        System.out.println("status       : " + retrieved.getStatus().getCode()
                + " / " + retrieved.getStatus().getLabel());
        System.out.println("transferType : " + retrieved.getTransferType().getCode()
                + " / " + retrieved.getTransferType().getLabel());
    }
}

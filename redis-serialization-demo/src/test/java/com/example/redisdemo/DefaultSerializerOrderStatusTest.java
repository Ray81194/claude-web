package com.example.redisdemo;

import com.example.redisdemo.dto.TransferDto;
import com.example.redisdemo.dto.type.AccountId;
import com.example.redisdemo.dto.type.Money;
import com.example.redisdemo.dto.type.OrderStatus;
import com.example.redisdemo.dto.type.TransferType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

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

    private TransferDto sampleDto() {
        TransferDto dto = new TransferDto();
        dto.setFromAccount(AccountId.of("AC-000001"));
        dto.setToAccount(AccountId.of("AC-000002"));
        dto.setAmount(Money.of(3000, "JPY"));
        dto.setStatus(OrderStatus.PENDING);
        dto.setTransferType(TransferType.EXPRESS);
        return dto;
    }

    @Test
    void testJsonFormat() throws Exception {
        redisTemplate.opsForValue().set(KEY, sampleDto());

        String rawJson = stringRedisTemplate.opsForValue().get(KEY);
        assertThat(rawJson).isNotNull();

        ObjectMapper mapper = new ObjectMapper();
        System.out.println("=== デフォルトシリアライザ JSON ===");
        System.out.println(mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(mapper.readValue(rawJson, Object.class)));

        // TransferDto は @class あり
        assertThat(rawJson).contains("\"@class\":\"com.example.redisdemo.dto.TransferDto\"");
        // AccountId はプレーン文字列
        assertThat(rawJson).contains("\"fromAccount\":\"AC-000001\"");
        assertThat(rawJson).contains("\"toAccount\":\"AC-000002\"");
        // Money は @class あり
        assertThat(rawJson).contains("\"@class\":\"com.example.redisdemo.dto.type.Money\"");
        assertThat(rawJson).contains("\"value\":3000");
        assertThat(rawJson).contains("\"currency\":\"JPY\"");
        // OrderStatus はコード文字列のみ（@class なし、ラベルなし）
        assertThat(rawJson).contains("\"status\":\"01\"");
        assertThat(rawJson).doesNotContain("\"@class\":\"com.example.redisdemo.dto.type.OrderStatus\"");
        assertThat(rawJson).doesNotContain("保留");
        // TransferType は @class あり・code フィールドのみ（ラベルなし）
        assertThat(rawJson).contains("\"@class\":\"com.example.redisdemo.dto.type.TransferType\"");
        assertThat(rawJson).contains("\"code\":\"02\"");
        assertThat(rawJson).doesNotContain("速達");
    }

    @Test
    void testDeserializedValues() {
        redisTemplate.opsForValue().set(KEY, sampleDto());

        TransferDto retrieved = (TransferDto) redisTemplate.opsForValue().get(KEY);
        System.out.println("=== デシリアライズ ===");
        System.out.println("status       : " + retrieved.getStatus().getCode()
                + " / " + retrieved.getStatus().getLabel());
        System.out.println("transferType : " + retrieved.getTransferType().getCode()
                + " / " + retrieved.getTransferType().getLabel());

        assertThat(retrieved).isNotNull();
        // AccountId
        assertThat(retrieved.getFromAccount()).isEqualTo(AccountId.of("AC-000001"));
        assertThat(retrieved.getToAccount()).isEqualTo(AccountId.of("AC-000002"));
        // Money
        assertThat(retrieved.getAmount().getValue()).isEqualTo(3000);
        assertThat(retrieved.getAmount().getCurrency()).isEqualTo("JPY");
        // OrderStatus - コード・ラベル・シングルトン同一性
        assertThat(retrieved.getStatus().getCode()).isEqualTo("01");
        assertThat(retrieved.getStatus().getLabel()).isEqualTo("保留");
        assertThat(retrieved.getStatus()).isSameAs(OrderStatus.PENDING);
        // TransferType - コード・ラベル・シングルトン同一性
        assertThat(retrieved.getTransferType().getCode()).isEqualTo("02");
        assertThat(retrieved.getTransferType().getLabel()).isEqualTo("速達");
        assertThat(retrieved.getTransferType()).isSameAs(TransferType.EXPRESS);
    }
}

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

import org.springframework.data.redis.serializer.SerializationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(rawJson).contains("\"@class\":\"com.example.redisdemo.dto.Money\"");
        assertThat(rawJson).contains("\"value\":3000");
        assertThat(rawJson).contains("\"currency\":\"JPY\"");
        // OrderStatus はコード文字列のみ（@class なし、ラベルなし）
        assertThat(rawJson).contains("\"status\":\"01\"");
        assertThat(rawJson).doesNotContain("\"@class\":\"com.example.redisdemo.dto.OrderStatus\"");
        assertThat(rawJson).doesNotContain("保留");
        // TransferType はコード文字列のみ（@class なし、ラベルなし）
        assertThat(rawJson).contains("\"transferType\":\"02\"");
        assertThat(rawJson).doesNotContain("\"@class\":\"com.example.redisdemo.dto.TransferType\"");
        assertThat(rawJson).doesNotContain("速達");
    }

    @Test
    void testDeserializedValues() {
        redisTemplate.opsForValue().set(KEY, sampleDto());

        // @JsonTypeInfo(use = NONE) を外した結果:
        // GenericJackson2JsonRedisSerializer はデフォルト型付きで動作するため、
        // プレーン文字列 "02" から @class を探そうとして InvalidTypeIdException が発生する。
        assertThatThrownBy(() -> redisTemplate.opsForValue().get(KEY))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Could not read JSON")
                .cause()
                .hasMessageContaining("transferType");
    }
}

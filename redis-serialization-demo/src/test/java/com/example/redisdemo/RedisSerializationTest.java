package com.example.redisdemo;

import com.example.redisdemo.dto.AccountId;
import com.example.redisdemo.dto.Money;
import com.example.redisdemo.dto.TransferDto;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RedisSerializationTest {

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

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
    void testBasicSerializeAndDeserialize() {
        TransferDto dto = new TransferDto();
        dto.setFromAccount(AccountId.of("AC-000001"));
        dto.setToAccount(AccountId.of("AC-000002"));
        dto.setAmount(Money.of(3000, "JPY"));

        redisTemplate.opsForValue().set(KEY, dto);

        Object result = redisTemplate.opsForValue().get(KEY);

        assertThat(result).isInstanceOf(TransferDto.class);
        TransferDto retrieved = (TransferDto) result;
        assertThat(retrieved.getFromAccount()).isEqualTo(AccountId.of("AC-000001"));
        assertThat(retrieved.getToAccount()).isEqualTo(AccountId.of("AC-000002"));
        assertThat(retrieved.getAmount()).isEqualTo(Money.of(3000, "JPY"));
    }

    @Test
    void testAccountIdsAreRestoredCorrectly() {
        TransferDto dto = new TransferDto();
        dto.setFromAccount(AccountId.of("AC-000001"));
        dto.setToAccount(AccountId.of("AC-000002"));
        dto.setAmount(Money.of(5000, "JPY"));

        redisTemplate.opsForValue().set(KEY, dto);

        TransferDto retrieved = (TransferDto) redisTemplate.opsForValue().get(KEY);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getFromAccount().getValue()).isEqualTo("AC-000001");
        assertThat(retrieved.getToAccount().getValue()).isEqualTo("AC-000002");
        // Verify they are not swapped
        assertThat(retrieved.getFromAccount()).isNotEqualTo(retrieved.getToAccount());
    }

    @Test
    void testMoneyIsRestoredCorrectly() {
        TransferDto dto = new TransferDto();
        dto.setFromAccount(AccountId.of("AC-000001"));
        dto.setToAccount(AccountId.of("AC-000002"));
        dto.setAmount(Money.of(3000, "JPY"));

        redisTemplate.opsForValue().set(KEY, dto);

        TransferDto retrieved = (TransferDto) redisTemplate.opsForValue().get(KEY);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getAmount().getValue()).isEqualTo(3000);
        assertThat(retrieved.getAmount().getCurrency()).isEqualTo("JPY");
    }

    @Test
    void testJsonFormatAccountIdIsPlainString() {
        TransferDto dto = new TransferDto();
        dto.setFromAccount(AccountId.of("AC-000001"));
        dto.setToAccount(AccountId.of("AC-000002"));
        dto.setAmount(Money.of(3000, "JPY"));

        redisTemplate.opsForValue().set(KEY, dto);

        // Get raw JSON from Redis using StringRedisTemplate
        String rawJson = stringRedisTemplate.opsForValue().get(KEY);

        assertThat(rawJson).isNotNull();
        // AccountId should be serialized as a plain string, not an object
        assertThat(rawJson).contains("\"fromAccount\":\"AC-000001\"");
        assertThat(rawJson).contains("\"toAccount\":\"AC-000002\"");
        // Money should be serialized as an object with @class
        assertThat(rawJson).contains("\"@class\":\"com.example.redisdemo.dto.Money\"");
        assertThat(rawJson).contains("\"value\":3000");
        assertThat(rawJson).contains("\"currency\":\"JPY\"");
        // TransferDto should have @class
        assertThat(rawJson).contains("\"@class\":\"com.example.redisdemo.dto.TransferDto\"");
    }
}

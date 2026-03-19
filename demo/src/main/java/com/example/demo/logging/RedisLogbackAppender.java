package com.example.demo.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ERROR ログを Redis リスト（app:logs:error）に JSON 文字列として記録する Logback Appender。
 * Spring コンテキストに依存せず、Lettuce クライアントを直接使用する。
 */
public class RedisLogbackAppender extends AppenderBase<ILoggingEvent> {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneId.of("Asia/Tokyo"));

    // logback-spring.xml からセットされるプロパティ
    private String redisHost  = "localhost";
    private int    redisPort  = 6379;
    private String redisKey   = "app:logs:error";
    private int    maxEntries = 1000;

    private RedisClient                           redisClient;
    private StatefulRedisConnection<String, String> connection;
    private RedisAsyncCommands<String, String>    commands;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---- Logback ライフサイクル ----

    @Override
    public void start() {
        try {
            redisClient = RedisClient.create("redis://" + redisHost + ":" + redisPort);
            connection  = redisClient.connect();
            commands    = connection.async();
        } catch (Exception e) {
            // addWarn にして起動を継続（addError は Spring Boot が起動失敗として扱うため使わない）
            addWarn("Redis 接続失敗（ログは Redis へ記録されません）: " + e.getMessage());
        }
        super.start();
    }

    @Override
    public void stop() {
        try {
            if (connection  != null) connection.close();
            if (redisClient != null) redisClient.shutdown();
        } catch (Exception ignored) {}
        super.stop();
    }

    // ---- ログ書き込み ----

    @Override
    protected void append(ILoggingEvent event) {
        if (commands == null || !isStarted()) return;
        try {
            Map<String, Object> entry = buildEntry(event);
            String json = objectMapper.writeValueAsString(entry);
            commands.lpush(redisKey, json)
                    .thenAccept(ignored -> commands.ltrim(redisKey, 0, maxEntries - 1))
                    .exceptionally(ex -> { addError("Redis ltrim 失敗", ex); return null; });
        } catch (Exception e) {
            addError("Redis へのログ書き込み失敗", e);
        }
    }

    private Map<String, Object> buildEntry(ILoggingEvent event) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp",  TIMESTAMP_FMT.format(Instant.ofEpochMilli(event.getTimeStamp())));
        entry.put("level",      event.getLevel().toString());
        entry.put("logger",     event.getLoggerName());
        entry.put("thread",     event.getThreadName());
        entry.put("message",    event.getFormattedMessage());
        entry.put("stackTrace", formatStackTrace(event.getThrowableProxy()));
        return entry;
    }

    private String formatStackTrace(IThrowableProxy proxy) {
        if (proxy == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(proxy.getClassName()).append(": ").append(proxy.getMessage()).append("\n");
        for (StackTraceElementProxy step : proxy.getStackTraceElementProxyArray()) {
            sb.append("\tat ").append(step.getSTEAsString()).append("\n");
        }
        if (proxy.getCause() != null) {
            sb.append("Caused by: ").append(formatStackTrace(proxy.getCause()));
        }
        return sb.toString();
    }

    // ---- Logback property setter ----

    public void setRedisHost(String redisHost)   { this.redisHost  = redisHost; }
    public void setRedisPort(int redisPort)       { this.redisPort  = redisPort; }
    public void setRedisKey(String redisKey)      { this.redisKey   = redisKey; }
    public void setMaxEntries(int maxEntries)     { this.maxEntries = maxEntries; }
}

package com.example.redislog.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Logback アペンダー: ログを Redis の LIST に LPUSH し、上限を超えたエントリを LTRIM で削除する。
 * Spring コンテキスト外でも動作するよう Jedis を直接使用する。
 */
public class RedisLogAppender extends AppenderBase<ILoggingEvent> {

    private Encoder<ILoggingEvent> encoder;
    private JedisPool jedisPool;

    private String host = "localhost";
    private int port = 6379;
    private String password = "";
    private String key = "app:logs";
    private int maxEntries = 1000;

    @Override
    public void start() {
        if (encoder == null) {
            addError("Encoder is required for RedisLogAppender");
            return;
        }
        encoder.start();

        redis.clients.jedis.JedisPoolConfig config = new redis.clients.jedis.JedisPoolConfig();
        config.setMaxTotal(8);
        config.setMaxIdle(4);
        config.setTestOnBorrow(true);

        if (password != null && !password.isBlank()) {
            jedisPool = new JedisPool(config, host, port, 2000, password);
        } else {
            jedisPool = new JedisPool(config, host, port, 2000);
        }

        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (jedisPool == null || jedisPool.isClosed()) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            byte[] encoded = encoder.encode(event);
            jedis.lpush(key.getBytes(), encoded);
            // 古いエントリを削除して上限を維持
            jedis.ltrim(key, 0, maxEntries - 1);
        } catch (Exception e) {
            addError("Failed to write log to Redis", e);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (encoder != null) {
            encoder.stop();
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    // --- setters (Logback XML から注入) ---

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }
}

package com.example.redislog.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/logs")
public class LogDemoController {

    private static final Logger log = LoggerFactory.getLogger(LogDemoController.class);
    private static final String LOG_KEY = "app:logs";

    private final StringRedisTemplate redisTemplate;

    public LogDemoController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** ログを出力するエンドポイント */
    @GetMapping("/write")
    public String writeLog(@RequestParam(defaultValue = "Hello Redis Log!") String message) {
        log.info("User message: {}", message);
        log.warn("Sample WARN log");
        log.error("Sample ERROR log");
        return "Logged: " + message;
    }

    /** Redis に保存されたログを取得するエンドポイント */
    @GetMapping
    public List<String> readLogs(@RequestParam(defaultValue = "20") int limit) {
        List<String> entries = redisTemplate.opsForList().range(LOG_KEY, 0, limit - 1);
        return entries != null ? entries : List.of();
    }
}

package com.example.demo.controller;

import com.example.demo.dto.NavigationSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class HomeController {

    // 画面コード定数
    static final String SCREEN_HOME         = "HOME";
    static final String SCREEN_REDIS        = "REDIS_SESSION";
    static final String SCREEN_SESSION_LIST = "SESSION_LIST";

    private static final String APP_SESSION_KEY_PREFIX = "app:session:";
    private static final Duration SESSION_TTL = Duration.ofSeconds(1800);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        NavigationSession navSession = loadNavSession(session.getId());
        navSession.addVisit(SCREEN_HOME);
        saveSession(session, navSession);

        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("sessionId", session.getId());
        model.addAttribute("visitCount", incrementVisitCount(session));
        model.addAttribute("navSession", navSession);
        return "home";
    }

    @GetMapping("/session/redis")
    public String redisSession(HttpSession session, Model model) {
        NavigationSession navSession = loadNavSession(session.getId());
        navSession.addVisit(SCREEN_REDIS);
        saveSession(session, navSession);

        String key = APP_SESSION_KEY_PREFIX + session.getId();
        Map<Object, Object> raw = stringRedisTemplate.opsForHash().entries(key);

        Map<String, Object> sessionData = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            sessionData.put(entry.getKey().toString(), entry.getValue());
        }

        model.addAttribute("sessionId", session.getId());
        model.addAttribute("redisKey", key);
        model.addAttribute("sessionData", sessionData);
        model.addAttribute("navSession", navSession);
        return "redis-session";
    }

    @GetMapping("/session/list")
    public String sessionList(HttpSession currentSession, Model model) {
        NavigationSession navSession = loadNavSession(currentSession.getId());
        navSession.addVisit(SCREEN_SESSION_LIST);
        saveSession(currentSession, navSession);

        Set<String> keys = stringRedisTemplate.keys(APP_SESSION_KEY_PREFIX + "*");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tokyo"));

        List<Map<String, String>> sessions = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> hash = stringRedisTemplate.opsForHash().entries(key);
                Map<String, String> info = new HashMap<>();
                String sid = key.replace(APP_SESSION_KEY_PREFIX, "");
                info.put("id", sid);
                info.put("current", sid.equals(currentSession.getId()) ? "true" : "false");
                info.put("username", valueOf(hash.get("username"), "（未ログイン）"));

                Object ct = hash.get("createdAt");
                Object la = hash.get("lastAccessedAt");
                info.put("creationTime",      ct != null ? fmt.format(Instant.ofEpochMilli(toLong(ct))) : "-");
                info.put("lastAccessedTime",  la != null ? fmt.format(Instant.ofEpochMilli(toLong(la))) : "-");
                sessions.add(info);
            }
        }

        model.addAttribute("sessions", sessions);
        model.addAttribute("currentSessionId", currentSession.getId());
        model.addAttribute("navSession", navSession);
        return "session-list";
    }

    @PostMapping("/session/delete/{sid}")
    public String deleteSession(@PathVariable String sid, HttpSession currentSession) {
        stringRedisTemplate.delete(APP_SESSION_KEY_PREFIX + sid);
        stringRedisTemplate.delete("spring:session:sessions:" + sid);
        if (sid.equals(currentSession.getId())) {
            currentSession.invalidate();
        }
        return "redirect:/session/list";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, HttpSession session) {
        session.setAttribute("username", username);
        stringRedisTemplate.opsForHash().put(APP_SESSION_KEY_PREFIX + session.getId(), "username", username);
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        stringRedisTemplate.delete(APP_SESSION_KEY_PREFIX + session.getId());
        session.invalidate();
        return "redirect:/";
    }

    // ---- ヘルパー ----

    private NavigationSession loadNavSession(String sessionId) {
        String key = APP_SESSION_KEY_PREFIX + sessionId;
        Object navJson = stringRedisTemplate.opsForHash().get(key, "nav");
        if (navJson == null) return new NavigationSession();
        try {
            return objectMapper.readValue(navJson.toString(), NavigationSession.class);
        } catch (Exception e) {
            return new NavigationSession();
        }
    }

    private void saveSession(HttpSession session, NavigationSession navSession) {
        String key = APP_SESSION_KEY_PREFIX + session.getId();
        try {
            stringRedisTemplate.opsForHash().putIfAbsent(key, "createdAt",
                    String.valueOf(System.currentTimeMillis()));
            stringRedisTemplate.opsForHash().put(key, "nav",
                    objectMapper.writeValueAsString(navSession));
            stringRedisTemplate.opsForHash().put(key, "username",
                    valueOf(session.getAttribute("username"), ""));
            stringRedisTemplate.opsForHash().put(key, "lastAccessedAt",
                    String.valueOf(System.currentTimeMillis()));
            stringRedisTemplate.expire(key, SESSION_TTL);
        } catch (Exception e) {
            throw new RuntimeException("セッション保存失敗", e);
        }
    }

    private int incrementVisitCount(HttpSession session) {
        Integer count = (Integer) session.getAttribute("visitCount");
        int newCount = (count == null) ? 1 : count + 1;
        session.setAttribute("visitCount", newCount);
        return newCount;
    }

    private String valueOf(Object o, String fallback) {
        return (o != null && !o.toString().isEmpty()) ? o.toString() : fallback;
    }

    private long toLong(Object o) {
        if (o instanceof Long l) return l;
        if (o instanceof Integer i) return i.longValue();
        return Long.parseLong(o.toString());
    }
}

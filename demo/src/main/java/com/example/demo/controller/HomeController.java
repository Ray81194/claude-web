package com.example.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class HomeController {

    @Autowired
    @Qualifier("sessionRedisTemplate")
    private RedisTemplate<String, Object> sessionRedisTemplate;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        model.addAttribute("sessionId", session.getId());
        model.addAttribute("visitCount", incrementVisitCount(session));
        return "home";
    }

    @GetMapping("/session/redis")
    public String redisSession(HttpSession session, Model model) {
        String sessionKey = "spring:session:sessions:" + session.getId();
        Map<Object, Object> entries = sessionRedisTemplate.opsForHash().entries(sessionKey);

        Map<String, Object> sessionData = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            sessionData.put(entry.getKey().toString(), entry.getValue());
        }

        model.addAttribute("sessionId", session.getId());
        model.addAttribute("redisKey", sessionKey);
        model.addAttribute("sessionData", sessionData);
        return "redis-session";
    }

    @GetMapping("/session/list")
    public String sessionList(HttpSession currentSession, Model model) {
        Set<String> keys = sessionRedisTemplate.keys("spring:session:sessions:*");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tokyo"));

        List<Map<String, String>> sessions = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                // expirations インデックスキーをスキップ
                if (key.contains(":expirations:")) continue;

                Map<Object, Object> hash = sessionRedisTemplate.opsForHash().entries(key);
                Map<String, String> info = new HashMap<>();
                String sid = key.replace("spring:session:sessions:", "");
                info.put("id", sid);
                info.put("current", sid.equals(currentSession.getId()) ? "true" : "false");
                info.put("username", valueOf(hash.get("sessionAttr:username"), "（未ログイン）"));

                Object ct = hash.get("creationTime");
                Object la = hash.get("lastAccessedTime");
                info.put("creationTime",  ct != null ? fmt.format(Instant.ofEpochMilli(toLong(ct))) : "-");
                info.put("lastAccessedTime", la != null ? fmt.format(Instant.ofEpochMilli(toLong(la))) : "-");
                sessions.add(info);
            }
        }

        model.addAttribute("sessions", sessions);
        model.addAttribute("currentSessionId", currentSession.getId());
        return "session-list";
    }

    @PostMapping("/session/delete/{sid}")
    public String deleteSession(@PathVariable String sid, HttpSession currentSession) {
        String key = "spring:session:sessions:" + sid;
        sessionRedisTemplate.delete(key);
        // 削除したのが自分自身なら新しいセッションへ
        if (sid.equals(currentSession.getId())) {
            currentSession.invalidate();
        }
        return "redirect:/session/list";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, HttpSession session) {
        session.setAttribute("username", username);
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    private int incrementVisitCount(HttpSession session) {
        Integer count = (Integer) session.getAttribute("visitCount");
        int newCount = (count == null) ? 1 : count + 1;
        session.setAttribute("visitCount", newCount);
        return newCount;
    }

    private String valueOf(Object o, String fallback) {
        return (o != null) ? o.toString() : fallback;
    }

    private long toLong(Object o) {
        if (o instanceof Long l) return l;
        if (o instanceof Integer i) return i.longValue();
        return Long.parseLong(o.toString());
    }
}

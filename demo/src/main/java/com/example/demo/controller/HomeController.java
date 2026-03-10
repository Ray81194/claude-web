package com.example.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

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
}

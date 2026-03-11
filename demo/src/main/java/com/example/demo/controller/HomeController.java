package com.example.demo.controller;

import com.example.demo.dto.AppSession;
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

    static final String SCREEN_HOME         = "HOME";
    static final String SCREEN_REDIS        = "REDIS_SESSION";
    static final String SCREEN_SESSION_LIST = "SESSION_LIST";

    private static final String APP_SESSION_KEY_PREFIX = "app:session:";
    private static final Duration SESSION_TTL = Duration.ofSeconds(1800);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // ---- エンドポイント ----

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        AppSession app = load(session.getId());
        app.getNavigation().addVisit(SCREEN_HOME);
        app.setVisitCount(app.getVisitCount() + 1);
        save(app);

        model.addAttribute("username",   app.getUsername());
        model.addAttribute("sessionId",  app.getSessionId());
        model.addAttribute("visitCount", app.getVisitCount());
        model.addAttribute("navSession", app.getNavigation());
        return "home";
    }

    @GetMapping("/session/redis")
    public String redisSession(HttpSession session, Model model) {
        AppSession app = load(session.getId());
        app.getNavigation().addVisit(SCREEN_REDIS);
        app.setVisitCount(app.getVisitCount() + 1);
        save(app);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tokyo"));

        Map<String, Object> sessionData = new LinkedHashMap<>();
        sessionData.put("sessionId",       app.getSessionId());
        sessionData.put("username",        blankOr(app.getUsername(), "（未ログイン）"));
        sessionData.put("visitCount",      app.getVisitCount());
        sessionData.put("createdAt",       fmt.format(Instant.ofEpochMilli(app.getCreatedAt())));
        sessionData.put("lastAccessedAt",  fmt.format(Instant.ofEpochMilli(app.getLastAccessedAt())));
        sessionData.put("navigation.size", app.getNavigation().size());
        if (app.getNavigation().getCurrentPage() != null) {
            sessionData.put("navigation.currentPage", app.getNavigation().getCurrentPage().getScreenCode());
        }

        model.addAttribute("sessionId",   app.getSessionId());
        model.addAttribute("redisKey",    APP_SESSION_KEY_PREFIX + app.getSessionId());
        model.addAttribute("sessionData", sessionData);
        model.addAttribute("navSession",  app.getNavigation());
        return "redis-session";
    }

    @GetMapping("/session/list")
    public String sessionList(HttpSession currentSession, Model model) {
        AppSession app = load(currentSession.getId());
        app.getNavigation().addVisit(SCREEN_SESSION_LIST);
        app.setVisitCount(app.getVisitCount() + 1);
        save(app);

        Set<String> keys = stringRedisTemplate.keys(APP_SESSION_KEY_PREFIX + "*");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("Asia/Tokyo"));

        List<Map<String, String>> sessions = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                String json = stringRedisTemplate.opsForValue().get(key);
                if (json == null) continue;
                try {
                    AppSession s = objectMapper.readValue(json, AppSession.class);
                    Map<String, String> info = new HashMap<>();
                    info.put("id",              s.getSessionId());
                    info.put("current",         s.getSessionId().equals(currentSession.getId()) ? "true" : "false");
                    info.put("username",        blankOr(s.getUsername(), "（未ログイン）"));
                    info.put("creationTime",    fmt.format(Instant.ofEpochMilli(s.getCreatedAt())));
                    info.put("lastAccessedTime",fmt.format(Instant.ofEpochMilli(s.getLastAccessedAt())));
                    sessions.add(info);
                } catch (Exception ignored) {}
            }
        }

        model.addAttribute("sessions",   sessions);
        model.addAttribute("navSession", app.getNavigation());
        return "session-list";
    }

    @PostMapping("/session/delete/{sid}")
    public String deleteSession(@PathVariable String sid, HttpSession currentSession) {
        // app:session のみ削除（Spring Session には触れない）
        stringRedisTemplate.delete(APP_SESSION_KEY_PREFIX + sid);
        if (sid.equals(currentSession.getId())) {
            currentSession.invalidate();
        }
        return "redirect:/session/list";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, HttpSession session) {
        AppSession app = load(session.getId());
        app.setUsername(username);
        save(app);
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        stringRedisTemplate.delete(APP_SESSION_KEY_PREFIX + session.getId());
        session.invalidate();
        return "redirect:/";
    }

    // ---- ヘルパー ----

    /** Redis から AppSession を読み込む。なければ新規作成。 */
    private AppSession load(String sessionId) {
        String json = stringRedisTemplate.opsForValue().get(APP_SESSION_KEY_PREFIX + sessionId);
        if (json != null) {
            try {
                AppSession s = objectMapper.readValue(json, AppSession.class);
                if (s.getNavigation() == null) s.setNavigation(new NavigationSession());
                return s;
            } catch (Exception ignored) {}
        }
        AppSession s = new AppSession();
        s.setSessionId(sessionId);
        s.setCreatedAt(System.currentTimeMillis());
        s.setNavigation(new NavigationSession());
        return s;
    }

    /** AppSession を JSON 文字列として Redis に保存する。 */
    private void save(AppSession app) {
        app.setLastAccessedAt(System.currentTimeMillis());
        try {
            String json = objectMapper.writeValueAsString(app);
            stringRedisTemplate.opsForValue().set(APP_SESSION_KEY_PREFIX + app.getSessionId(), json, SESSION_TTL);
        } catch (Exception e) {
            throw new RuntimeException("セッション保存失敗", e);
        }
    }

    private String blankOr(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}

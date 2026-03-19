package com.example.demo.dto;

/**
 * コントローラーが Redis に JSON 文字列として保存するセッションデータ
 * （Spring Session の内部構造とは完全に独立）
 */
public class AppSession {

    private String sessionId;
    private String username;
    private int visitCount;
    private long createdAt;
    private long lastAccessedAt;
    private NavigationSession navigation;

    public AppSession() {}

    public String getSessionId()                 { return sessionId; }
    public void   setSessionId(String v)         { this.sessionId = v; }

    public String getUsername()                  { return username; }
    public void   setUsername(String v)          { this.username = v; }

    public int  getVisitCount()                  { return visitCount; }
    public void setVisitCount(int v)             { this.visitCount = v; }

    public long getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(long v)             { this.createdAt = v; }

    public long getLastAccessedAt()              { return lastAccessedAt; }
    public void setLastAccessedAt(long v)        { this.lastAccessedAt = v; }

    public NavigationSession getNavigation()     { return navigation; }
    public void setNavigation(NavigationSession v){ this.navigation = v; }
}

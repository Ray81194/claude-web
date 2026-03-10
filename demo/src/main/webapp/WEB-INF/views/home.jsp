<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Spring Boot + Redis Session Demo</title>
    <style>
        body { font-family: sans-serif; max-width: 600px; margin: 40px auto; padding: 0 20px; }
        nav { margin-bottom: 24px; }
        nav a { margin-right: 16px; color: #4a90e2; text-decoration: none; }
        nav a.active { font-weight: bold; color: #333; }
        .card { border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
        .label { font-weight: bold; color: #555; }
        input[type=text] { padding: 6px 10px; border: 1px solid #ccc; border-radius: 4px; }
        button { padding: 7px 16px; background: #4a90e2; color: white; border: none; border-radius: 4px; cursor: pointer; }
        button.logout { background: #e25454; }
        .history-list { list-style: none; padding: 0; margin: 0; }
        .history-list li { padding: 4px 0; border-bottom: 1px solid #f0f0f0; font-size: 0.9em; display: flex; gap: 12px; }
        .history-list li:last-child { border-bottom: none; }
        .screen-code { font-weight: bold; color: #4a90e2; min-width: 130px; }
        .visited-at { color: #888; }
    </style>
</head>
<body>
    <nav>
        <a href="/" class="active">ホーム</a>
        <a href="/session/redis">Redis セッション値</a>
        <a href="/session/list">セッション管理</a>
    </nav>

    <h1>Spring Boot 3 + Redis Session</h1>

    <div class="card">
        <p><span class="label">セッションID:</span> ${sessionId}</p>
        <p><span class="label">アクセス回数:</span> ${visitCount} 回</p>
    </div>

    <c:choose>
        <c:when test="${not empty username}">
            <div class="card">
                <p>ようこそ、<strong>${username}</strong> さん！</p>
                <form method="post" action="/logout">
                    <button type="submit" class="logout">ログアウト</button>
                </form>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card">
                <h3>ログイン</h3>
                <form method="post" action="/login">
                    <input type="text" name="username" placeholder="ユーザー名" required />
                    <button type="submit">ログイン</button>
                </form>
            </div>
        </c:otherwise>
    </c:choose>

    <div class="card">
        <h3>画面遷移履歴</h3>
        <p style="font-size:0.85em;color:#888">セッションキー: <code>navigationSession</code> &nbsp;|&nbsp; 件数: ${navSession.size()}</p>
        <c:choose>
            <c:when test="${not empty navSession.history}">
                <ul class="history-list">
                    <c:forEach var="visit" items="${navSession.history}">
                        <li>
                            <span class="screen-code">${visit.screenCode}</span>
                            <span class="visited-at">${visit.visitedAt}</span>
                        </li>
                    </c:forEach>
                </ul>
            </c:when>
            <c:otherwise><p style="color:#888;margin:0">履歴なし</p></c:otherwise>
        </c:choose>
    </div>
</body>
</html>

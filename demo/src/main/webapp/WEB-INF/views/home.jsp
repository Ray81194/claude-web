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
        .card { border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
        .label { font-weight: bold; color: #555; }
        input[type=text] { padding: 6px 10px; border: 1px solid #ccc; border-radius: 4px; }
        button { padding: 7px 16px; background: #4a90e2; color: white; border: none; border-radius: 4px; cursor: pointer; }
        button.logout { background: #e25454; }
    </style>
</head>
<body>
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
</body>
</html>

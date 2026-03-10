<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Redis セッション値</title>
    <style>
        body { font-family: sans-serif; max-width: 700px; margin: 40px auto; padding: 0 20px; }
        nav { margin-bottom: 24px; }
        nav a { margin-right: 16px; color: #4a90e2; text-decoration: none; }
        nav a.active { font-weight: bold; color: #333; }
        .card { border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
        .label { font-weight: bold; color: #555; }
        table { width: 100%; border-collapse: collapse; }
        th, td { text-align: left; padding: 8px 12px; border-bottom: 1px solid #eee; }
        th { background: #f5f5f5; }
        code { background: #f0f0f0; padding: 2px 6px; border-radius: 3px; font-size: 0.9em; word-break: break-all; }
        a { color: #4a90e2; text-decoration: none; }
    </style>
</head>
<body>
    <nav>
        <a href="/">ホーム</a>
        <a href="/session/redis" class="active">Redis セッション値</a>
        <a href="/session/list">セッション管理</a>
    </nav>

    <h1>Redis セッション値</h1>

    <div class="card">
        <p><span class="label">セッションID:</span> <code>${sessionId}</code></p>
        <p><span class="label">Redis キー:</span> <code>${redisKey}</code></p>
    </div>

    <div class="card">
        <h3>セッション属性</h3>
        <c:choose>
            <c:when test="${not empty sessionData}">
                <table>
                    <thead>
                        <tr><th>キー</th><th>値</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="entry" items="${sessionData}">
                            <tr>
                                <td><code>${entry.key}</code></td>
                                <td><code>${entry.value}</code></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise>
                <p>セッションデータがありません（セッションが空か、まだ保存されていません）</p>
            </c:otherwise>
        </c:choose>
    </div>

</body>
</html>

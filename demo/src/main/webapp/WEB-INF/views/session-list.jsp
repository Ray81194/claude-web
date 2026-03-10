<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>セッション管理</title>
    <style>
        body { font-family: sans-serif; max-width: 900px; margin: 40px auto; padding: 0 20px; }
        nav { margin-bottom: 24px; }
        nav a { margin-right: 16px; color: #4a90e2; text-decoration: none; }
        nav a.active { font-weight: bold; color: #333; }
        .card { border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
        table { width: 100%; border-collapse: collapse; }
        th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #eee; }
        th { background: #f5f5f5; }
        tr.current-session { background: #eef6ff; }
        code { background: #f0f0f0; padding: 2px 6px; border-radius: 3px; font-size: 0.85em; word-break: break-all; }
        .badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 0.8em; background: #4a90e2; color: white; }
        button.del { padding: 4px 12px; background: #e25454; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 0.85em; }
        .empty { color: #888; }
        .history-list { list-style: none; padding: 0; margin: 0; }
        .history-list li { padding: 4px 0; border-bottom: 1px solid #f0f0f0; font-size: 0.9em; display: flex; gap: 12px; }
        .history-list li:last-child { border-bottom: none; }
        .screen-code { font-weight: bold; color: #4a90e2; min-width: 130px; }
        .visited-at { color: #888; }
    </style>
</head>
<body>
    <nav>
        <a href="/">ホーム</a>
        <a href="/session/redis">Redis セッション値</a>
        <a href="/session/list" class="active">セッション管理</a>
    </nav>

    <h1>セッション管理</h1>

    <div class="card">
        <p>Redis 上のアクティブセッション一覧です。<span class="badge">現在のセッション</span> 行が自分のセッションです。</p>
    </div>

    <div class="card">
        <c:choose>
            <c:when test="${not empty sessions}">
                <table>
                    <thead>
                        <tr>
                            <th>セッションID</th>
                            <th>ユーザー</th>
                            <th>作成日時</th>
                            <th>最終アクセス</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="s" items="${sessions}">
                            <tr class="${s.current == 'true' ? 'current-session' : ''}">
                                <td>
                                    <code>${s.id}</code>
                                    <c:if test="${s.current == 'true'}">
                                        &nbsp;<span class="badge">現在のセッション</span>
                                    </c:if>
                                </td>
                                <td>${s.username}</td>
                                <td>${s.creationTime}</td>
                                <td>${s.lastAccessedTime}</td>
                                <td>
                                    <form method="post" action="/session/delete/${s.id}"
                                          onsubmit="return confirm('セッション ${s.id} を削除しますか？')">
                                        <button type="submit" class="del">削除</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise>
                <p class="empty">アクティブなセッションがありません。</p>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card">
        <h3>画面遷移履歴</h3>
        <c:choose>
            <c:when test="${not empty navigationHistory}">
                <ul class="history-list">
                    <c:forEach var="visit" items="${navigationHistory}">
                        <li>
                            <span class="screen-code">${visit.screenCode}</span>
                            <span class="visited-at">${visit.visitedAt}</span>
                        </li>
                    </c:forEach>
                </ul>
            </c:when>
            <c:otherwise><p style="color:#888">履歴なし</p></c:otherwise>
        </c:choose>
    </div>
</body>
</html>

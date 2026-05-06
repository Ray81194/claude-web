<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>JSP Classpath Bug Reproduction</title>
</head>
<body>
    <h1>Bug Reproduction: JSP from Dependency Subproject</h1>
    <p>Message: <c:out value="${message}"/></p>
    <p>このページが表示されれば JSP はクラスパスにある（bootRun）。</p>
    <p>500 / JSP compilation error になれば再現成功（VSCode Dashboard）。</p>
</body>
</html>

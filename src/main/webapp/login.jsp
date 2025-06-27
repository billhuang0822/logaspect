<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>登入頁面</title>
</head>
<body>
    <h2>請登入</h2>
    <form action="/api/login" method="post">
        <label>帳號：<input type="text" name="username" required /></label><br/>
        <label>密碼：<input type="password" name="password" required /></label><br/>
        <button type="submit">登入</button>
    </form>
</body>
</html>
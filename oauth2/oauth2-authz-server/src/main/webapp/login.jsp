<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>Contrail OAuth Authorization Server Login</title>
</head>
<body>
<h1>Contrail OAuth Authorization Server</h1>
<div>
    <div id="login-box">

        <h2>Login</h2>
        <%
            if (request.getParameter("error") != null) {
                String message = request.getParameter("error");
        %>
        <p style="color:red;"><%= message %>
        </p>
        <%
            }
        %>
        Please enter your username and password in the fields below and then click the 'Login' button.
        <br/>
        <br/>

        <form method="post" action="doLogin">
            <table>
                <tr>
                    <td>Username</td>
                </tr>
                <tr>
                    <td><input name="username" title="Username" value="" size="30" maxlength="100"/></td>
                </tr>
                <tr>
                    <td>Password</td>
                </tr>
                <tr>
                    <td><input name="password" type="password" title="Password" value="" size="30"
                               maxlength="100"/></td>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                </tr>
                <tr>
                    <td><input type="submit" name="submit" value="Login"/></td>
                </tr>
            </table>
            <input name="return_to" type="hidden" value="<%= request.getParameter("return_to") %>"/>
        </form>
    </div>
</div>
</body>
</html>

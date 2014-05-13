<%@ page import="org.ow2.contrail.common.oauth.client.TokenInfo" %>
<%@ page import="org.ow2.contrail.common.oauth.demo.utils.TokenUtils" %>
<%@ page import="java.util.UUID" %>
<%
    String accessToken = (String) session.getAttribute("access_token");
    String tokenInfo = null;
    if (accessToken != null) {
        try {
            tokenInfo = TokenUtils.getTokenInfo(accessToken).toJson().toString(3);
        }
        catch (Exception e) {
            throw new Exception("Failed to validate access token: " + e.getMessage());
        }
    }

    String state = UUID.randomUUID().toString();
    String authorizationRequestUri = TokenUtils.getAuthorizationRequestUri(state);
    session.setAttribute("state", state);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>OAuth-Java-Client-Demo</title>
</head>
<body>
<h1>Contrail OAuth Client Demo</h1>
<%
    if (request.getParameter("error") != null) {
%>
<p style="color:red;"><%= request.getParameter("error") %>
</p>
<%
    }
%>

<%
    if (request.getParameter("message") != null) {
%>
<p style="color:green;"><%= request.getParameter("message") %>
</p>
<%
    }
%>


<strong>Access token:</strong>
<%= accessToken != null ? accessToken : "not available" %>
<br/>
<%
    if (tokenInfo != null) {
%>
Token info:
<pre><%= tokenInfo %></pre>
<%
    }
%>
<p>
    <strong>Request a new access token:</strong>
    <br/>
    By clicking the link you will be redirected to the Authorization Server.
    Please give permission to OAuth-Java-Client-Demo application to access your account.
    <br/>
    <a href="<%= authorizationRequestUri %>">Request an access token</a>
</p>
<%
    if (tokenInfo != null) {
%>
<h3>Access protected resources</h3>
<a href="get_cert.jsp">Request user certificate</a>
<%
    }
%>
</body>
</html>

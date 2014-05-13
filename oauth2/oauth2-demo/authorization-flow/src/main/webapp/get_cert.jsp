<%@ page import="org.ow2.contrail.common.oauth.client.KeyAndCertificate" %>
<%@ page import="org.ow2.contrail.common.oauth.demo.utils.CertUtils" %>
<%@ page import="org.ow2.contrail.common.oauth.demo.utils.Conf" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String accessToken = (String) session.getAttribute("access_token");
    if (accessToken == null) {
        response.sendRedirect("get_token.jsp");
    }

    KeyAndCertificate keyAndCertificate = CertUtils.retrieveCert(accessToken);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>OAuth-Java-Client-Demo</title>
</head>
<body>
<h1>Contrail OAuth Client Demo</h1>

<p>
    Requesting user certificate from the CA (<%= Conf.getInstance().getCAUserCertUri() %>).
    <br/>
    Using access token: <%= accessToken %>
</p>

<p>
    The certificate has been retrieved successfully.<br/>
    Private key:
    <pre>
    <%= CertUtils.convertToPem(keyAndCertificate.getPrivateKey()) %>
    </pre>
    <br/>
    Certificate:
    <pre>
    <%= CertUtils.convertToPem(keyAndCertificate.getCertificate()) %>
    </pre>
</p>
<a href="get_token.jsp">Back</a>
</body>
</html>

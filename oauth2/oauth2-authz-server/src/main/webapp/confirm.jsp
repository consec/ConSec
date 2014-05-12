<%@ page import="org.consec.oauth2.authzserver.jpa.entities.Client" %>
<%@ page import="org.consec.oauth2.authzserver.jpa.entities.Country" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Iterator" %>
<%@page contentType="text/html" %>
<%@page pageEncoding="UTF-8" %>
<%
    Client client = (Client) request.getAttribute("client");
    String returnTo = (String) request.getAttribute("return_to");
    @SuppressWarnings("unchecked")
    HashSet<String> scopes = (HashSet<String>) request.getAttribute("scopes");
    StringBuilder countriesList = new StringBuilder();
    if (client.getCountryList() != null && client.getCountryList().size() > 0) {
        Iterator<Country> it = client.getCountryList().iterator();
        while (it.hasNext()) {
            Country country = it.next();
            if (it.hasNext()) {
                countriesList.append(", ");
            }
            countriesList.append(country.getName());
        }
    }
    else {
        countriesList.append("unknown");
    }
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>Contrail OAuth Authorization Server</title>
</head>
<body>

<div style="text-align:right; font-size:small">Logged in as: <%= session.getAttribute("owner_uuid") %></div>
<h1>Contrail OAuth Authorization Server</h1>
<div>
    <div id="authorize-box">

        <h2>Request for Permission</h2>
        The application <strong><%= client.getName() %></strong>
        from the organization <%= client.getOrganization().getName() %>,
        located in following countries: <%= countriesList.toString() %>
        <br/>
        is requesting permission to:
        <%
            for (String scope : scopes) {
        %>
        <ul>
            <li><%= scope %>
            </li>
        </ul>
        <%
            }
        %>
        <br/>
        <br/>

        <form name="authorizeForm" method="post" action="<%= returnTo %>">
            <table>
                <tr>
                    <td><input type="submit" name="consent" value="Allow access"/></td>
                    <td><input type="submit" name="consent" value="Deny access"/></td>
                </tr>
                <tr>
                    <td>
                        <input type="checkbox" name="justThisTime" value="true"/>Just this time
                    </td>
                </tr>
            </table>
        </form>
    </div>
</div>
</body>
</html>

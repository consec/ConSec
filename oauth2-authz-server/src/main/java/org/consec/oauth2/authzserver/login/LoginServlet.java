package org.consec.oauth2.authzserver.login;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jettison.json.JSONObject;
import org.consec.oauth2.authzserver.jpa.dao.OwnerDao;
import org.consec.oauth2.authzserver.jpa.entities.Owner;
import org.consec.oauth2.authzserver.jpa.enums.OwnerType;
import org.consec.oauth2.authzserver.utils.Configuration;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String returnTo = request.getParameter("return_to");

        // authenticate user using federation-api
        try {
            HttpPost postRequest = new HttpPost(Configuration.getInstance().getAuthenticateServiceUri());
            postRequest.addHeader("Content-Type", "application/json");
            JSONObject credentials = new JSONObject();
            credentials.put("username", username);
            credentials.put("password", password);
            HttpEntity postEntity = new StringEntity(credentials.toString(), "UTF-8");
            postRequest.setEntity(postEntity);

            HttpClient client = new DefaultHttpClient();
            HttpResponse postResponse = client.execute(postRequest);

            if (postResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                postResponse.getEntity().writeTo(baos);
                String json = baos.toString();
                JSONObject userData = new JSONObject(json);
                String uuid = userData.getString("uuid");

                // retrieve resource owner
                EntityManager em = null;
                Owner owner;
                try {
                    em = PersistenceUtils.getInstance().getEntityManager();
                    owner = new OwnerDao(em).findByUuid(uuid);
                    if (owner == null) {
                        owner = new Owner();
                        owner.setUuid(uuid);
                        owner.setOwnerType(OwnerType.USER);

                        em.getTransaction().begin();
                        em.persist(owner);
                        em.getTransaction().commit();
                    }
                }
                finally {
                    PersistenceUtils.getInstance().closeEntityManager(em);
                }

                HttpSession session = request.getSession(true);
                session.setAttribute("authenticated", true);
                session.setAttribute("owner_uuid", owner.getUuid());
                response.sendRedirect(returnTo);
            }
            else {
                String message = "Login failed: invalid username or password.";
                QueryStringBuilder builder = new QueryStringBuilder();
                builder.add("return_to", returnTo);
                builder.add("error", message);
                response.sendRedirect("login.jsp?" + builder.getQueryString());
            }
        }
        catch (Exception e) {
            throw new ServletException("Failed to authenticate user: " + e.getMessage());
        }
    }

    static class QueryStringBuilder {
        private String queryString = "";

        public void add(String name, String value) throws UnsupportedEncodingException {
            if (!queryString.equals("")) {
                queryString += "&";
            }
            queryString += name + "=" + URLEncoder.encode(value, "UTF-8");
        }

        public String getQueryString() {
            return queryString;
        }
    }
}

package org.ow2.contrail.common.oauth.demo;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.ow2.contrail.common.oauth.demo.utils.Conf;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

public class OAuth2CallbackEndpoint extends HttpServlet {
    private static Logger log = Logger.getLogger(Conf.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            String code = request.getParameter("code");
            if (code != null) {
                HttpSession session = request.getSession();

                // check state
                String state = request.getParameter("state");
                String originalState = (String) session.getAttribute("state");
                if (state == null || !state.equals(originalState)) {
                    throw new Exception("State parameter mismatch.");
                }

                // obtain an access token from the Authorization Server using the authorization code
                URI requestTokenUri = new URI(Conf.getInstance().getASAccessTokenUri());
                HttpPost httpPost = new HttpPost(requestTokenUri);

                List<NameValuePair> formParams = new ArrayList<NameValuePair>();
                formParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
                formParams.add(new BasicNameValuePair("code", code));
                formParams.add(new BasicNameValuePair("redirect_uri", Conf.getInstance().getClientOauth2CallbackUri()));
                formParams.add(new BasicNameValuePair("client_id", Conf.getInstance().getClientId()));
                formParams.add(new BasicNameValuePair("client_secret", Conf.getInstance().getClientSecret()));
                formParams.add(new BasicNameValuePair("scope", Conf.getInstance().getScope()));

                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
                httpPost.setEntity(entity);

                // read in the keystore from the filesystem, this should contain a single keypair
                KeyStore clientKeyStore = KeyStore.getInstance("JKS");
                clientKeyStore.load(new FileInputStream(Conf.getInstance().getClientKeystoreFile()),
                        Conf.getInstance().getClientKeystorePass().toCharArray());

                // read in the truststore from the filesystem, this should contain a single keypair
                KeyStore trustStore = KeyStore.getInstance("JKS");
                trustStore.load(new FileInputStream(Conf.getInstance().getClientTruststoreFile()),
                        Conf.getInstance().getClientTruststorePass().toCharArray());

                // set up the socketfactory, to use our keystore for client authentication.
                SSLSocketFactory socketFactory = new SSLSocketFactory(
                        SSLSocketFactory.TLS,
                        clientKeyStore,
                        Conf.getInstance().getClientKeystorePass(),
                        trustStore,
                        null,
                        null,
                        SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

                // create and configure scheme registry
                SchemeRegistry registry = new SchemeRegistry();
                registry.register(new Scheme("https", requestTokenUri.getPort(), socketFactory));

                // create a client connection manager to use in creating httpclients
                ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(registry);

                if (log.isDebugEnabled()) {
                    log.debug("Sending request to Authorization Server: " + requestTokenUri);
                    OutputStream baos = new ByteArrayOutputStream();
                    entity.writeTo(baos);
                    log.debug("Request entity: " + baos.toString());
                }

                // create the client based on the manager, and use it to make the call
                HttpClient httpClient = new DefaultHttpClient(mgr);
                HttpResponse httpResponse = httpClient.execute(httpPost);

                // parse response
                HttpEntity responseEntity = httpResponse.getEntity();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                responseEntity.writeTo(baos);
                String content = baos.toString();

                if (log.isDebugEnabled()) {
                    log.debug("Received response from Authorization Server.");
                    log.debug("Status: " + httpResponse.getStatusLine().toString());
                    log.debug("Content: " + content);
                }

                JSONObject json = null;
                try {
                    json = new JSONObject(content);
                    log.debug("JSON parsed successfully.");
                }
                catch (JSONException e) {
                    throw new Exception("Invalid response received from the Authorization Server: " + content);
                }

                if (json.has("error")) {
                    String message = "Error response received from the Authorization Server: " + json.getString("error");
                    if (json.has("error_description")) {
                        message += ": " + json.getString("error_description");
                    }
                    throw new Exception(message);
                }
                else if (json.has("access_token")) {
                    String accessToken = json.getString("access_token");
                    session.setAttribute("access_token", accessToken);

                    String message = "Access token was retrieved successfully.";
                    log.debug(message);
                    String url = "get_token.jsp?message=" + URLEncoder.encode(message, "UTF-8");
                    response.sendRedirect(url);
                    return;
                }
                else {
                    throw new Exception("Unexpected response received from the Authorization Server: " + content);
                }
            }
            else {
                String error = request.getParameter("error");
                String errorDesc = request.getParameter("error_description");
                if (error != null) {
                    String message = "Error response received from the Authorization Server: " + error;
                    if (errorDesc != null) {
                        message += ": " + errorDesc;
                    }
                    throw new Exception(message);
                }
                else {
                    throw new Exception("Invalid response from the Authorization Server.");
                }
            }
        }
        catch (Exception e) {
            String message = "Failed to obtain access token: " + e.getMessage();
            log.error(message, e);
            String url = "get_token.jsp?error=" + URLEncoder.encode(message, "UTF-8");
            response.sendRedirect(url);
        }
    }
}

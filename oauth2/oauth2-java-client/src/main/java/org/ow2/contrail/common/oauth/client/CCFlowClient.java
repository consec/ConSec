package org.ow2.contrail.common.oauth.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONException;
import org.json.JSONObject;
import org.ow2.contrail.common.oauth.client.exceptions.UnauthorizedException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CCFlowClient {
    private static Logger log = Logger.getLogger(CCFlowClient.class);
    private URI tokenEndpointUri;
    private String keystoreFile;
    private String keystorePass;
    private String truststoreFile;
    private String truststorePass;
    private String clientId;
    private String clientSecret;

    public CCFlowClient(URI tokenEndpointUri) throws Exception {
        if (tokenEndpointUri.getScheme().equals("https")) {
            throw new Exception("Keystore and truststore are required for https connection.");
        }
        this.tokenEndpointUri = tokenEndpointUri;
    }

    public CCFlowClient(URI tokenEndpointUri,
                        String keystoreFile, String keystorePass,
                        String truststoreFile, String truststorePass) {
        this.tokenEndpointUri = tokenEndpointUri;
        this.keystoreFile = keystoreFile;
        this.keystorePass = keystorePass;
        this.truststoreFile = truststoreFile;
        this.truststorePass = truststorePass;

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public AccessToken requestAccessToken(String resourceOwner, String scope) throws Exception {

        // prepare post request
        HttpPost httpPost = new HttpPost(tokenEndpointUri);

        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("grant_type", "client_credentials"));
        if (resourceOwner != null) {
            formParams.add(new BasicNameValuePair("resource_owner", resourceOwner));
        }
        if (scope != null) {
            formParams.add(new BasicNameValuePair("scope", scope));
        }

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
        httpPost.setEntity(entity);

        // set Authorization header
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(clientId, clientSecret);
        httpPost.addHeader(BasicScheme.authenticate(creds, "UTF-8", false));

        HttpClient httpClient;
        if (tokenEndpointUri.getScheme().equals("https")) {
            // read in the keystore from the filesystem, this should contain a single keypair
            KeyStore clientKeyStore = KeyStore.getInstance("JKS");
            clientKeyStore.load(new FileInputStream(keystoreFile), keystorePass.toCharArray());

            // read in the truststore from the filesystem, this should contain a single keypair
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(truststoreFile), truststorePass.toCharArray());

            // set up the socketfactory, to use our keystore for client authentication.
            SSLSocketFactory socketFactory = new SSLSocketFactory(
                    SSLSocketFactory.TLS,
                    clientKeyStore,
                    keystorePass,
                    trustStore,
                    null,
                    null,
                    SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

            // create and configure scheme registry
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("https", tokenEndpointUri.getPort(), socketFactory));

            // create a client connection manager to use in creating httpclients
            ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(registry);

            // create the client based on the manager, and use it to make the call
            httpClient = new DefaultHttpClient(mgr);
        }
        else {
            httpClient = new DefaultHttpClient();
        }

        // send post request to the Authorization Server
        HttpResponse httpResponse = httpClient.execute(httpPost);

        // parse response
        HttpEntity responseEntity = httpResponse.getEntity();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        responseEntity.writeTo(baos);
        String content = baos.toString();

        if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            JSONObject json = null;
            try {
                json = new JSONObject(content);
            }
            catch (JSONException e) {
                String message = String.format(
                        "Invalid response received from the Authorization Server: %s", e.getMessage());
                log.error(message + ": " + content, e);
                throw new Exception(message);
            }

            AccessToken accessToken = new AccessToken();
            accessToken.setValue(json.getString("access_token"));
            Date now = new Date();
            int expiresIn = json.getInt("expires_in");
            Date expireTime = new Date(now.getTime() + expiresIn * 1000);
            accessToken.setExpiresIn(expiresIn);
            accessToken.setExpireTime(expireTime);
            log.debug("Access token has been obtained successfully: " + accessToken.toJson());
            return accessToken;
        }
        else if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
            JSONObject json = null;
            try {
                json = new JSONObject(content);
            }
            catch (JSONException e) {
                String message = String.format(
                        "Invalid response received from the Authorization Server: %s", e.getMessage());
                log.error(message + ": " + content, e);
                throw new Exception(message);
            }

            String message = "Error response received from the Authorization Server: " + json.getString("error");
            if (json.has("error_description")) {
                message += ": " + json.getString("error_description");
            }
            log.debug(message);
            throw new UnauthorizedException(message);
        }
        else if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
            String message = "The request for access token has been refused" +
                    ((content != null) ? (": " + content) : ".");
            log.debug(message);
            throw new UnauthorizedException(message);
        }
        else {
            String message = "Unexpected response from the Authorization server: " + httpResponse.getStatusLine();
            log.debug(message + ": " + content);
            throw new Exception(message);
        }
    }
}

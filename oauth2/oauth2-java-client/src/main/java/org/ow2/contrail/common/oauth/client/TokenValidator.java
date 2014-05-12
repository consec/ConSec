package org.ow2.contrail.common.oauth.client;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenValidator {
    private URI endpointUri;
    private String keystoreFile;
    private String keystorePass;
    private String truststoreFile;
    private String truststorePass;

    public TokenValidator(URI endpointUri, String keystoreFile, String keystorePass,
                          String truststoreFile, String truststorePass) {
        this.endpointUri = endpointUri;
        this.keystoreFile = keystoreFile;
        this.keystorePass = keystorePass;
        this.truststoreFile = truststoreFile;
        this.truststorePass = truststorePass;
    }

    public TokenInfo checkToken(HttpServletRequest httpRequest) throws Exception {
        // extract access token from the Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null) {
            throw new TokenValidator.InvalidOAuthTokenException("The Authorization header is missing.");
        }

        Pattern authHeaderPattern = Pattern.compile("^Bearer ([\\w-]+)$");
        Matcher m = authHeaderPattern.matcher(authHeader);
        if (!m.find()) {
            throw new TokenValidator.InvalidOAuthTokenException("Invalid Authorization header.");
        }
        String accessToken = m.group(1);

        // get client name from the certificate
        // client - an application making protected resource requests on behalf of the resource owner
        X509Certificate[] certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            throw new TokenValidator.InvalidCertificateException("The client certificate was not provided.");
        }

        String clientId = certs[0].getSubjectDN().getName();
        return checkToken(accessToken, clientId);
    }

    public TokenInfo checkToken(String accessToken, String bearerId) throws Exception {

        HttpPost postRequest = new HttpPost(endpointUri);
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("access_token", accessToken));
        formParams.add(new BasicNameValuePair("bearer_id", bearerId));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
        postRequest.setEntity(entity);

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
        registry.register(new Scheme("https", endpointUri.getPort(), socketFactory));

        // create a client connection manager to use in creating httpclients
        ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(registry);

        // create the client based on the manager, and use it to make the call
        HttpClient httpClient = new DefaultHttpClient(mgr);
        HttpResponse response = httpClient.execute(postRequest);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            response.getEntity().writeTo(baos);
            String json = baos.toString();

            try {
                return new TokenInfo(json);
            }
            catch (Exception e) {
                throw new Exception(String.format(
                        "Invalid response received from the OAuth authorization server '%s': %s",
                        endpointUri, e.getMessage()));
            }
        }
        else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            throw new InvalidOAuthTokenException("You are not authorized to access the requested resource.");
        }
        else {
            throw new Exception(String.format(
                    "Unexpected response received from the OAuth authorization server '%s': %s",
                    endpointUri, response.getStatusLine().toString()));
        }
    }

    public static class InvalidOAuthTokenException extends Exception {
        public InvalidOAuthTokenException(String message) {
            super(message);
        }
    }

    public static class InvalidCertificateException extends Exception {
        public InvalidCertificateException(String message) {
            super(message);
        }
    }
}

package org.ow2.contrail.common.oauth.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.Security;
import java.util.Scanner;

public class OAuthHttpClient {
    private static Logger log = Logger.getLogger(OAuthHttpClient.class);

    private String keystoreFile;
    private String keystorePass;
    private String truststoreFile;
    private String truststorePass;

    public OAuthHttpClient(String keystoreFile, String keystorePass,
                           String truststoreFile, String truststorePass) {
        this.keystoreFile = keystoreFile;
        this.keystorePass = keystorePass;
        this.truststoreFile = truststoreFile;
        this.truststorePass = truststorePass;

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public HttpResponse get(URI uri, String accessToken) throws Exception {

        if (!uri.getScheme().equals("https")) {
            throw new Exception("Https connection is required.");
        }

        HttpGet request = new HttpGet(uri);
        request.setHeader("Authorization", String.format("Bearer %s", accessToken));

        HttpClient httpClient = createHttpClient(uri);
        return httpClient.execute(request);
    }

    public HttpResponse post(URI uri, String accessToken, HttpEntity entity) throws Exception {

        if (!uri.getScheme().equals("https")) {
            throw new Exception("Https connection is required.");
        }
        HttpPost request = new HttpPost(uri);

        request.setHeader("Authorization", String.format("Bearer %s", accessToken));

        request.setEntity(entity);

        HttpClient httpClient = createHttpClient(uri);
        return httpClient.execute(request);
    }

    public HttpResponse put(URI uri, String accessToken, HttpEntity entity) throws Exception {

        if (!uri.getScheme().equals("https")) {
            throw new Exception("Https connection is required.");
        }
        HttpPut request = new HttpPut(uri);

        request.setHeader("Authorization", String.format("Bearer %s", accessToken));

        request.setEntity(entity);

        HttpClient httpClient = createHttpClient(uri);
        return httpClient.execute(request);
    }

    public HttpResponse delete(URI uri, String accessToken) throws Exception {

        if (!uri.getScheme().equals("https")) {
            throw new Exception("Https connection is required.");
        }
        HttpDelete request = new HttpDelete(uri);

        request.setHeader("Authorization", String.format("Bearer %s", accessToken));

        HttpClient httpClient = createHttpClient(uri);
        return httpClient.execute(request);
    }

    public String getContent(HttpResponse response) throws IOException {
        Scanner scanner = new Scanner(response.getEntity().getContent(), "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private HttpClient createHttpClient(URI uri) throws Exception {
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
        registry.register(new Scheme("https", uri.getPort(), socketFactory));

        // create a client connection manager to use in creating httpclients
        ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(registry);

        // create the client based on the manager, and use it to make the call
        return new DefaultHttpClient(mgr);
    }
}

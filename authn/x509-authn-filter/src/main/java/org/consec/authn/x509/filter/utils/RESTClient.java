package org.consec.authn.x509.filter.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.Security;
import java.util.Scanner;

public class RESTClient {
    private static Logger log = Logger.getLogger(RESTClient.class);

    private String endpoint;
    private String keystoreFile;
    private String keystorePass;
    private String truststoreFile;
    private String truststorePass;

    public RESTClient(String endpoint, String keystoreFile, String keystorePass,
                      String truststoreFile, String truststorePass) {
        this.endpoint = endpoint;
        this.keystoreFile = keystoreFile;
        this.keystorePass = keystorePass;
        this.truststoreFile = truststoreFile;
        this.truststorePass = truststorePass;

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public String get(String path) throws Exception {
        URI address = new URI(endpoint + path);
        HttpGet request = new HttpGet(address);
        HttpClient httpClient = createHttpClient(address);
        HttpResponse httpResponse = httpClient.execute(request);

        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            Scanner scanner = new Scanner(httpResponse.getEntity().getContent(), "UTF-8").useDelimiter("\\A");
            return scanner.next();
        }
        else if (httpResponse.getStatusLine().getStatusCode() == 404) {
            return null;
        }
        else {
            throw new Exception("Invalid response received from the federation-api: " + httpResponse.getStatusLine());
        }
    }

    private org.apache.http.client.HttpClient createHttpClient(URI uri) throws Exception {
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

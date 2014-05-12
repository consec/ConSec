package org.ow2.contrail.common.oauth.client;

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
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;
import org.ow2.contrail.common.oauth.client.utils.CertUtils;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CertRetriever {
    private static Logger log = Logger.getLogger(CertRetriever.class);

    private URI endpointUri;
    private String keystoreFile;
    private String keystorePass;
    private String truststoreFile;
    private String truststorePass;

    public CertRetriever(URI endpointUri) throws Exception {
        if (endpointUri.getScheme().equals("https")) {
            throw new Exception("Keystore and truststore are required for https connection.");
        }
        this.endpointUri = endpointUri;
    }

    public CertRetriever(URI endpointUri,
                         String keystoreFile, String keystorePass,
                         String truststoreFile, String truststorePass) {
        this.endpointUri = endpointUri;
        this.keystoreFile = keystoreFile;
        this.keystorePass = keystorePass;
        this.truststoreFile = truststoreFile;
        this.truststorePass = truststorePass;

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public KeyAndCertificate retrieveCert(String accessToken) throws Exception {
        log.debug("Retrieving user certificate from the ca-server at " + endpointUri);
        log.debug("Generating private/public key pair and CSR.");
        KeyPair keyPair = CertUtils.generateKeyPair("RSA", 2048);
        // TODO: subject?
        String subject = String.format("CN=%s", "TestUser");
        PKCS10CertificationRequest csr = CertUtils.createCSR(keyPair, subject, "SHA256withRSA");

        log.debug("Encoding CSR into PEM format.");
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        pemWriter.writeObject(csr);
        pemWriter.flush();
        pemWriter.close();

        String pemEncoded = writer.toString();

        HttpPost request = new HttpPost(endpointUri);
        log.debug("Using access token: " + accessToken);
        request.addHeader("Authorization", String.format("Bearer %s", accessToken));
        request.addHeader("Accept-Encoding", "identity");

        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("certificate_request", pemEncoded));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
        request.setEntity(entity);

        HttpClient httpClient;
        if (endpointUri.getScheme().equals("https")) {
            log.debug("Setting up SSL-enabled HttpClient. ");
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
            httpClient = new DefaultHttpClient(mgr);
        }
        else {
            log.debug("Setting up non-SSL HttpClient.");
            httpClient = new DefaultHttpClient();
        }

        log.debug("Sending request to " + endpointUri);
        HttpResponse response = httpClient.execute(request);
        log.debug("Received response from the ca-server: " + response.getStatusLine());
        HttpEntity responseEntity = response.getEntity();

        if (response.getStatusLine().getStatusCode() != 200) {
            Scanner scanner = new Scanner(responseEntity.getContent()).useDelimiter("\\A");
            String content = scanner.hasNext() ? scanner.next() : "No content in response.";
            log.error(String.format("Unexpected response from the ca-server:\\n%s\\n%s",
                    response.getStatusLine(), content));
            throw new Exception("Unexpected response from the ca-server: " + response.getStatusLine());
        }

        InputStreamReader isr = null;
        X509Certificate cert;
        try {
            log.debug("Loading certificate from PEM.");
            isr = new InputStreamReader(responseEntity.getContent());
            cert = CertUtils.readCertificate(isr);
            log.debug("The certificate was obtained successfully.");

            return new KeyAndCertificate(keyPair.getPrivate(), cert);
        }
        catch (Exception e) {
            log.error("Invalid response received from the ca-server - failed to load certificate from PEM.", e);
            throw new Exception("Invalid response received from the ca-server - failed to load certificate from PEM: " +
                    e.getMessage());
        }
        finally {
            if (isr != null)
                isr.close();
        }
    }
}

package org.ow2.contrail.common.oauth.client;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.security.Security;

@Ignore
public class CertRetrieverTest {

    /**
     * Retrieves user certificate from CA using oauth access token
     */
    @Test
    public void testRetrieveCert() throws Exception {
        URI endpoint = new URI("http://localhost:9080/ca/o/delegateduser");
        String accessToken = "1391fbe6-fef0-369f-942c-4a61657e84ea";

        Security.addProvider(new BouncyCastleProvider());

        CertRetriever certRetriever = new CertRetriever(endpoint);
        KeyAndCertificate keyAndCertificate = certRetriever.retrieveCert(accessToken);

        System.out.println(keyAndCertificate.getCertificate());
    }

    /**
     * Retrieves user certificate from CA using oauth access token though https
     */
    @Test
    public void testRetrieveCertHttps() throws Exception {
        URI endpoint = new URI("https://localhost:9443/ca/o/delegateduser");
        String accessToken = "1391fbe6-fef0-369f-942c-4a61657e84ea";
        String keystoreFile = "src/test/resources/client.jks";
        String keystorePass = "contrail";
        String truststoreFile = "src/test/resources/cacerts.jks";
        String truststorePass = "contrail";

        Security.addProvider(new BouncyCastleProvider());

        CertRetriever certRetriever = new CertRetriever(endpoint, keystoreFile, keystorePass,
                truststoreFile, truststorePass);
        KeyAndCertificate keyAndCertificate = certRetriever.retrieveCert(accessToken);

        System.out.println(keyAndCertificate.getCertificate());
    }
}

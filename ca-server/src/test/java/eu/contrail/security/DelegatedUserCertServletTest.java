///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package eu.contrail.security;
//
package eu.contrail.security;

import com.meterware.httpunit.Base64;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author ijj
 */
public class DelegatedUserCertServletTest {

    private SecurityCommons sc = new SecurityCommons();
    final String fakeCSR =
            "-----BEGIN CERTIFICATE REQUEST-----\n"
                    + "MIICUTCCATkCAQAwDjEMMAoGA1UEAxMDaWpqMIIBIjANBgkqhkiG9w0BAQEFAAOC\n"
                    + "AQ8AMIIBCgKCAQEAph95NgnO/ewdTTJvsUqwSRgWA7dDCzBEj3hAcXRnHD2S0SG7\n"
                    + "XkOyM4QDT5nT6f0E7rD1d6xXOgQ8pP6vNMbs+M6O6cSvSWx0uiWSfd4Iy/DLjR6/\n"
                    + "+WPDkYcpDftFUgbon10S+NGG569ZrVln6tLUL+MIVebuN9mRd4ZOCJgwKmvX4yPx\n"
                    + "T9GgKe/cvIqWIZPJnsDbyP9vUKgF3IISPVn3NrtDIvCjc4mQJXWG1syWmDImVqcp\n"
                    + "zviVitiSqanDXLLnMO6N//Rmq7zMcM5I8CrZHihpBkeIW4HwYECuKEmYd9YU2lxy\n"
                    + "833F7EOe3VvsUYcMfEW3zMW0w/4F0uOzW96wMQIDAQABMA0GCSqGSIb3DQEBCwUA\n"
                    + "A4IBAQAlwQgcR7utM7OqGhAlYqQC4nLqcoJ69loYaFpnhfMwD+a4vzKYwGbapldg\n"
                    + "0OWc28ND3l8TQ7cfEKTlPK2DPQOz4wcSa+i+NyijU343QNI5Gg1stBe7H4QNgOq+\n"
                    + "/FfTgm9EoruogRpIV4RuFOAwRJI0ksRVpQxpO02lNQGR6MKO9GkwjFJkfcNtSK/P\n"
                    + "03SH+h0ANXKYf2tjfwyTi+uWoT2fMpysYQSzo1plpa2+dZ7p0UZYzwvDkTwT321u\n"
                    + "Lny/2zdge3HyCP1yHmFvXQRLTX58nwAEFGk8S5dSMddNqJE0C98f1WbM2mORzNWk\n"
                    + "lMPz4dtWSfu6SAzEqQsMArVxmkK5\n"
                    + "-----END CERTIFICATE REQUEST-----\n";
    static ServletRunner sr = null;
    static ServletUnitClient client = null;

    public DelegatedUserCertServletTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {


        try {

            Security.addProvider(new BouncyCastleProvider());
            sr = new ServletRunner();
            sr.registerServlet("/delegateduser", UserCertServlet.class.getName());

            sr = new ServletRunner(new File("src/test/webapp/WEB-INF/web.xml"), "/ca");
//      PersistenceUtils.createInstance("appPU");


        }
        catch (Exception ex) {

            System.err.println(ex);

        }

    }

    //  @AfterClass
    public static void tearDownClass() throws Exception {
//    if (sr != null) {
//      
//      try {
//      sr.shutDown();
//      
//      } catch (Exception ex) {
//        
//        System.err.println("tearDownClass()");
//        
//        System.err.println("MSG = " + ex.getLocalizedMessage());
//        
//        ex.printStackTrace();
//        
//      }
//    }    
    }

    @Before
    public void setUp() {

        client = sr.newClient();

    }

    //  @After
    public void tearDown() {
    }

    public static String createBasicAuthHeader(final String username, final String password) {


        String packed = (username + ":" + password);

        String enc = com.meterware.httpunit.Base64.encode(packed);

        return "Basic " + enc;


    }

    @Test
    public void testDummy() throws Exception {
        System.out.println("dummy");
    }

    //  @Test
    public void testBasicAuth() throws Exception {

        System.out.println("basicAuth");

        String packed = "validUser" + ":" + "validPassword";

        String authHeader = createBasicAuthHeader("validUser", "validPassword");

        String pair[] = authHeader.split(" ");

        if (pair != null) {

            String enc = pair[1];

            String decoded = Base64.decode(enc);

            assertEquals(packed, decoded);


        }

    }

    private void print_content(HttpsURLConnection con) {
        if (con != null) {

            try {

                System.out.println("****** Content of the URL ********");
                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(con.getInputStream()));

                String input;

                while ((input = br.readLine()) != null) {
                    System.out.println(input);
                }
                br.close();

            }
            catch (IOException e) {
                e.printStackTrace();
            }


        }

    }

    /*
     * TODO: Should be an integration test
     */
//  @Test
    public void testClientCertAuth() throws Exception {

        System.out.println("clientCertAuth");


        String https_url = "https://one-test.contrail.rl.ac.uk:8443/ca/delegateduser";
        URL url;

        final String homeDir = System.getProperty("user.home");

        System.setProperty("javax.net.ssl.keyStore", homeDir + "/mykeystore_03.p12");
        System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.keyStorePassword", "client");
        System.setProperty("javax.net.debug", "all");

        try {

            url = new URL(https_url);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();


            //dumpl all cert info
            print_https_cert(con);

            print_local_cert(con);

            //dump all the content
            print_content(con);

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    // @Test
    public void testCreateCSR() throws Exception {

        System.out.println("CreateCSR");


        final KeyPair keyPair = sc.generateKeyPair("RSA", 2048);

        PKCS10CertificationRequest request;

        final String userID = "CN=1";

        request = sc.createCSR(keyPair, userID, "SHA256withRSA");

        Assert.assertEquals(request.getCertificationRequestInfo().getSubject().toString(), String.format("CN=%s", "1"));


    }

    private void print_local_cert(HttpsURLConnection con) {

        Certificate[] local_certs = con.getLocalCertificates();

        if (local_certs != null) {

            System.out.println("Local certs:");
            int i = 0;
            for (Certificate cert : local_certs) {

                try {
                    X509Certificate c = X509Certificate.getInstance(cert.getEncoded());
                    System.out.printf("Cert %d: Subject %s.%n", i++, sc.reverse(c.getSubjectDN().getName(), ","));
                }
                catch (CertificateEncodingException ex) {
                    Logger.getLogger(DelegatedUserCertServletTest.class.getName()).log(Level.SEVERE, null, ex);
                }
                catch (CertificateException ex) {
                }

            }
        }
    }

    private void print_https_cert(HttpsURLConnection con) {

        if (con != null) {

            try {

                System.out.println("Response Code : " + con.getResponseCode());
                System.out.println("Cipher Suite : " + con.getCipherSuite());
                System.out.println("Server certificate chain:%n");

                Certificate[] certs = con.getServerCertificates();

                int i = 0;

                for (Certificate cert : certs) {


                    try {
                        X509Certificate c = X509Certificate.getInstance(cert.getEncoded());
                        System.out.printf("Cert %d: Subject %s.%n", i++, sc.reverse(c.getSubjectDN().getName(), ","));
                    }
                    catch (CertificateEncodingException ex) {
                        Logger.getLogger(DelegatedUserCertServletTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    catch (CertificateException ex) {
                    }


                    System.out.println("%n");
                }

            }
            catch (SSLPeerUnverifiedException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
}

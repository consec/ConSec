package eu.contrail.security;

import com.meterware.httpunit.*;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author ijj
 */
public class UserCertServletTest {
    private static final String TARGET_URL = "http://nonesuch:8080/ca/user";


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
    private static ServletRunner sr = null;

    private static ServletUnitClient client = null;


    public UserCertServletTest() {
    }

    //  @BeforeClass
    public static void setUpClass() throws Exception {


        try {

            final String filename = "src/test/webapp/WEB-INF/web.xml";

            File webXml = new File(filename);

            if (!webXml.exists()) {

                System.err.println(String.format("File %s does not exist", filename));
            }

            sr = new ServletRunner(webXml, "/ca");

        }
        catch (Exception ex) {

            ex.printStackTrace(System.err);

        }

    }

    @AfterClass
    public static void tearDownClass() throws Exception {


    }

    //  @Before
    public void setUp() {


        client = sr.newClient();

    }

    @After
    public void tearDown() {
    }

    public static String createBasicAuthHeader(final String username, final String password) {


        String packed = (username + ":" + password);

        String enc = com.meterware.httpunit.Base64.encode(packed);

        return "Basic " + enc;


    }

    @Test
    public void dummy() {
        System.out.println("dummy");
    }

    //  @Test
    public void testBasicAuthEncDec() throws Exception {

        System.out.println("basicAuthEncDec");

        String packed = "validUser" + ":" + "validPassword";

        String authHeader = createBasicAuthHeader("validUser", "validPassword");

        String pair[] = authHeader.split(" ");

        if (pair != null) {

            String enc = pair[1];

            String decoded = com.meterware.httpunit.Base64.decode(enc);

            assertEquals(packed, decoded);


        }

    }


    //  @Test
    public void testGetReturns405() {

        System.out.println("getreturns405\n");

        WebResponse response = null;

        try {

            WebRequest request = new GetMethodWebRequest(TARGET_URL);

            if (client == null) {

                System.err.println("client is NULL");
            }

            response = client.getResponse(request);

        }
        catch (HttpException ex) {

            final int responseCode = ex.getResponseCode();

            assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, responseCode);

        }
        catch (SAXException ex) {
            System.err.println(ex);
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
        catch (AuthorizationRequiredException ex) {
            ;
        }
    }

    //  @Test
    public void testPostNoAuth() {

        System.out.println("\nPostNoAuth\n");

        WebResponse response = null;

        try {

            WebRequest request = new PostMethodWebRequest(TARGET_URL);

            response = client.getResponse(request);
            fail("Expected an AuthorizationRequiredException");

        }
        catch (AuthorizationRequiredException ex) {

            System.out.println("ARE");
            System.err.println(ex.getLocalizedMessage());

            //      System.out.println("REALM=" + ex.getAuthenticationParameter("realm"));
//      ;

        }
        catch (HttpException ex) {

            System.out.println("HttpException");
            System.err.println(ex.getLocalizedMessage());

            //TODO: Should fail() line below

            System.err.println("Expected an AuthorizationRequiredException, not an HttpException");

//      final int responseCode = ex.getResponseCode();
//      
//
//      assertEquals(HttpServletResponse.SC_UNAUTHORIZED, responseCode);
//
//      System.out.println(ex.getResponseMessage());

        }
        catch (SAXException ex) {
            fail("Expected an AuthorizationRequiredException, not an SAXException");
        }
        catch (IOException ex) {
            fail("Expected an AuthorizationRequiredException, not an IOException");
        }
    }

    //  @Test
    public void testPostEmptyBasicAuth() {

        System.out.println("\nPostEmptyBasicAuth\n");

        WebResponse response = null;

        try {

            WebRequest request = new PostMethodWebRequest(TARGET_URL);

            final String username = "";
            final String password = "";

            final String basicAuthHeader = createBasicAuthHeader(username, password);
            client.setHeaderField("Authorization", basicAuthHeader);

            response = client.getResponse(request);

        }
        catch (AuthorizationRequiredException ex) {
//
        }
        catch (HttpException ex) {
            System.err.println(ex.getResponseMessage());
//      fail("Expected an AuthorizationRequiredException");

//      final int responseCode = ex.getResponseCode();
//
//      assertEquals(HttpServletResponse.SC_UNAUTHORIZED, responseCode);
//


        }
        catch (SAXException ex) {
            System.err.println(ex);
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
    }

    //  @Test
    public void testPostBasicAuthUsernameNoPassword() {

        System.out.println("\nPostEmptyBasicAuth\n");

        WebResponse response = null;

        try {

            WebRequest request = new PostMethodWebRequest(TARGET_URL);

            final String username = "username";
            final String password = "";

            final String basicAuthHeader = createBasicAuthHeader(username, password);
            client.setHeaderField("Authorization", basicAuthHeader);

            response = client.getResponse(request);

        }
        catch (AuthorizationRequiredException ex) {
            System.err.print(ex);
//ij
        }
        catch (HttpException ex) {
            System.err.println(ex.getResponseMessage());
//      fail("Expected an AuthorizationRequiredException");

//      final int responseCode = ex.getResponseCode();
//
//      assertEquals(HttpServletResponse.SC_UNAUTHORIZED, responseCode);
//


        }
        catch (SAXException ex) {
            System.err.println(ex);
        }
        catch (IOException ex) {
            System.err.println(ex);
        }
    }


    //  @Test
    public void testPostValidBasicAuthNoCSRParameter() throws Exception {

        System.out.println("\nPostValidBasicAuthNoCSRParameter");

        WebResponse response = null;
        WebRequest request = new PostMethodWebRequest(TARGET_URL);

        try {

            final String username = "validuser";
            final String password = "validpassword";

            final String inBasic = username + ":" + password;

            final int inputLength = inBasic.length();


//      byte[] inbytes = inBasic.getBytes();

            final String basicAuthHeader = createBasicAuthHeader(username, password);

            client.setHeaderField("Authorization", basicAuthHeader);
            final String authHeader = client.getHeaderField("Authorization");
            final String[] auth = authHeader.split(" ");


            final String decodedAuth = Base64.decode(auth[1]);

            assertEquals("Basic validuser:validpassword", "Basic " + decodedAuth);

            response = client.getResponse(request);


        }
        catch (HttpException ex) {

            final int responseCode = ex.getResponseCode();

            assertEquals(HttpServletResponse.SC_BAD_REQUEST, responseCode);

        }

    }

    //  @Test
    public void testPostValidBasicAuthEmptyCSRParameter() throws Exception {

        System.out.println("\nPostValidBasicAuthEmptyCSR\n");

//    final String webXmlFilename = "/opt/CA/web.xml";
        WebResponse response = null;
        WebRequest request = new PostMethodWebRequest(TARGET_URL);


        try {

            final String username = "validuser";
            final String password = "validpassword";

            final String basicAuthHeader = createBasicAuthHeader(username, password);

            client.setHeaderField("Authorization", basicAuthHeader);

            request.setParameter("certificate_request", "");

            response = client.getResponse(request);

        }
        catch (HttpException ex) {

            final int responseCode = ex.getResponseCode();

            assertEquals(HttpServletResponse.SC_BAD_REQUEST, responseCode);

            assertEquals("The request is missing the parameter 'certificate_request'", ex.getResponseMessage());

        }

    }


    //  @Test
    public void testPostValidBasicAuthInvalidCSR() throws Exception {

        System.out.println("\nPostValidBasicAuthInvalidCSR\n");

        WebResponse response = null;
        WebRequest request = new PostMethodWebRequest(TARGET_URL);

        try {

            final String username = "validuser";
            final String password = "validpassword";

            final String basicAuthHeader = createBasicAuthHeader(username, password);

            client.setHeaderField("Authorization", basicAuthHeader);

            request.setParameter("certificate_request", "ThisIsNotACSR");

            response = client.getResponse(request);

        }
        catch (HttpException ex) {

//      System.err.println("ValidPostInvalidCSR_Param\n" + ex.getLocalizedMessage());
            final int responseCode = ex.getResponseCode();

            assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseCode);

//      System.err.println(ex.getResponseMessage());

        }

    }

    //  @Test
    public void testPostValidBasicAuthValidCSR() throws Exception {

        System.out.println("PostValidBasicAuthValidCSR");

        WebResponse response = null;

        InputStream is = null;

        WebRequest request = new PostMethodWebRequest(TARGET_URL);

        try {

            final String username = "contrailuser";
            final String password = "password";

            final String basicAuthHeader = createBasicAuthHeader(username, password);

            client.setHeaderField("Authorization", basicAuthHeader);

            request.setParameter("certificate_request", fakeCSR);

            response = client.getResponse(request);

            System.out.println("Requested certificate from CA:");
            System.out.println(response.getText());

        }
        catch (HttpException ex) {

//      System.err.println("ValidPostValidCSR_Param\n" + ex.getLocalizedMessage());
            final int responseCode = ex.getResponseCode();
//      System.err.println(ex.getResponseMessage());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, responseCode);

        }

    }
}



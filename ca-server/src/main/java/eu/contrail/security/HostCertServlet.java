/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.contrail.security;

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * @author ijj
 */
public class HostCertServlet extends HttpServlet {

    private boolean debug = false;
    //  private final int DEFAULT_LIFETIME_DAYS = 365;
    private final int DEFAULT_LIFETIME_HOURS = 6;
    //  private String daysString;
    private String hoursString;
    //  private int days;
    private int hours;

    private static ServletContext ctx;
    private static PrivateKey issuerKey;
    private static String issuerKeyPairFilename;
    private char[] issuerKeyPairPassword;
    private static X509Certificate issuerCertificate;
    private static String issuerCertificateFilename;

    private static String issuerName;

    private String daysString;

    private int days;

    //  private String caIssuerName;
//  private X500Name caIssuerX500Name;
//  private X500Name issuerOrderedX500Name;
    private static BigInteger bigSerial;


//  private static  

    private PKCS10CertificationRequest getCSR(final HttpServletRequest request, SecurityCommons sc)
            throws IOException {

        PKCS10CertificationRequest csr = null;

        final String CSR_HEADER_NAME = "certificate_request";

        final String certificate_request = request.getParameter(CSR_HEADER_NAME);

        if (certificate_request == null) {

            if (debug) {
                ctx.log(String.format("Request Parameter %s is NULL", CSR_HEADER_NAME));
            }

        }
        else {

            final int csr_length = certificate_request.length();

//      if (debug) {
//        ctx.log(String.format("CSR = %d bytes long", certificate_request.length()));
//      }

            if (csr_length != 0) {
                csr = sc.readCSR(new ByteArrayInputStream(certificate_request.getBytes()));
            }

        }

        return csr;

    }

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    protected void processRequest(
            final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException {

        SecurityCommons sc = new SecurityCommons();

        String pathInfo = request.getPathInfo();

        if (debug) {
            if (pathInfo != null && !"".equals(pathInfo.trim())) {
                ctx.log(String.format("HCS: PathInfo = %s.", pathInfo));
            }
        }


        final User user = (User) request.getAttribute("user");

    /*
     * Sanity check - username must not be null
     *
     */


        if (user == null) {

            response.sendError(HttpServletResponse.SC_BAD_GATEWAY /*
         * UNAUTHORIZED
         */);
            if (debug) {
                ctx.log("HCS: User object attribute is NULL - should have been set by BasicAuthFilter");
            }

            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing authorization");
            return;

        }
        else {

            if (debug) {
                ctx.log(String.format("HCS: Username %s", user.getUsername()));
            }
        }

        //   final int userID = user.getUserId();

        //   final String uuid = user.getUuid();

//    ctx.log("HCS: UUID " + uuid);

        PrintWriter out = null;

        String hostname = null;

        try {

            final PKCS10CertificationRequest certRequest = getCSR(request, sc);

            if (certRequest == null) {

                if (debug) {
                    ctx.log("HCS: BLAST. Read CSR == NULL");
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The request is missing the parameter 'certificate_request'");
                return;

            }

            // TODO: check length of request public key - define allowable lengths
            // TODO: check key type - only allow RSA?

            try {
                certRequest.verify();
            }

            catch (InvalidKeyException ex) {

                final String errMsg = "HCS: Cannot verify CSR - InvalidKeyException - ignoring request";
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;

            }

            catch (SignatureException ex) {

                final String errMsg = "HCS: Cannot verify CSR - SignatureException - ignoring request";
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;

            }

            catch (Exception ex) {

                final String errMsg = "HCS: " + ex.getLocalizedMessage();
                ctx.log(errMsg);


                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;

            }

            hostname = certRequest.getCertificationRequestInfo().getSubject().toString();

            X509Certificate cert = null;


            final int minutes = 0;


            ctx.log("HCS: about to createUserCert");


//      final String subjectName = String.format("CN=%s, CN=%s", userID, user.getUsername());    

            //    final X500Principal subjectPrincipal = new X500Principal(issuerName + "," + subjectName);


            cert = sc.createHostCertificate(
                    certRequest.getPublicKey(),
//        new X500Principal(String.format("CN=%d, CN=%s", userID, user.getUsername())), uuid,
                    hostname,

                    bigSerial,
                    issuerCertificate,
                    issuerKey,
                    "SHA1withRSA",
                    new Date(),
                    days, hours, minutes);

            if (cert == null) {

                if (debug) {
                    ctx.log("HCS: Certificate from String == NULL");
                }

                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create a certificate");
                return;
            }


            if (debug) {
                ctx.log("HCS: About to write cert");
            }

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            sc.writeCertificate(new OutputStreamWriter(baos), cert);

            final String certString = baos.toString();

            response.setContentType("text/plain");
            response.setContentLength(certString.length()); // TO-DO: Try not setting content-length
            // In combination with not testing for is.available == 0 in client

            out = response.getWriter();
            out.write(certString);

//      if (debug) {
//        ctx.log("Finished writing cert");
//      }

            bigSerial = bigSerial.add(BigInteger.ONE);

        }
        catch (CertificateException ex) {

            ctx.log(ex.getLocalizedMessage());
            throw new ServletException(ex.getLocalizedMessage());


        }
        catch (IOException ex) {

            ctx.log(ex.getLocalizedMessage());

            // throw new ServletException(ex.getLocalizedMessage());

        }
        catch (InvalidKeyException ex) {

            ctx.log(ex.getLocalizedMessage());
            throw new ServletException(ex.getLocalizedMessage());

        }
        catch (NoSuchAlgorithmException ex) {

            ctx.log(ex.getLocalizedMessage());

            throw new ServletException(ex.getLocalizedMessage());

        }
        catch (OperatorCreationException ex) {

            ctx.log(ex.getLocalizedMessage());
            throw new ServletException(ex.getLocalizedMessage());

        }
        catch (NoSuchProviderException ex) {

            ctx.log(ex.getLocalizedMessage());
            throw new ServletException(ex.getLocalizedMessage());

        }
        catch (NullPointerException ex) {
            ctx.log(ex.getLocalizedMessage());

            ex.printStackTrace(out);

            throw new ServletException(ex.getLocalizedMessage());

        }

        catch (Exception ex) {

            ctx.log(ex.getLocalizedMessage());

            ex.printStackTrace(out);
        }

        finally {

            if (out != null) {

                try {
                    out.close();
                }
                catch (Exception ex) {
                    ;
                }
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Use POST instead");

        if (ctx != null) { // If running under ServletRunner without init params defined
            ctx.log("Ignoring GET request");
        }
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "User CA Servlet";
    }// </editor-fold>

    private int paramsMissing = 0;

    private void getInitParams(final ServletConfig config) {


        debug = Boolean.valueOf(config.getInitParameter("debug"));

        issuerKeyPairFilename = config.getInitParameter("issuerKeyPairFilename");

        if (issuerKeyPairFilename != null) {

            if (debug) {

                ctx.log(String.format("issuerKeyPairFilename = %s.", issuerKeyPairFilename));
            }

        }
        else {

            paramsMissing++;
            ctx.log("Cannot read property issuerKeyPairFilename.");

        }

        final String keyPass = config.getInitParameter("issuerKeyPairPassword");

        if (keyPass != null) {

            issuerKeyPairPassword = keyPass.toCharArray();

        }
        else {

            paramsMissing++;
            ctx.log("Cannot read property issuerKeyPairPassword.");

        }

        if (debug) {

            ctx.log(String.format("issuerKeyPairPassword is %s",
                    issuerKeyPairPassword == null ? "not set." : "set, but not logged here."));

        }

        issuerCertificateFilename = config.getInitParameter("issuerCertificateFilename");

        if (issuerCertificateFilename != null) {

            if (debug) {

                ctx.log(String.format("issuerCertificateFilename = %s.", issuerCertificateFilename));
            }

        }
        else {

            paramsMissing++;
            ctx.log("Cannot read property issuerCertificateFilename.");


        }

        hoursString = config.getInitParameter("certLifetimeHours");

        if (hoursString == null) {
            hours = 0;
        }
        else {
            hours = Integer.valueOf(hoursString);
            hours = Math.max(hours, 0);
        }

        daysString = config.getInitParameter("certLifetimeDays");

        if (daysString == null) {
            days = 0;
        }
        else {
            days = Integer.valueOf(daysString);
            days = Math.max(days, 0);
        }

        if (debug) {

            ctx.log(String.format("HCS: certificate duration is %d days, %d hours", days, hours));

        }

    }

    @Override
    public void init(final ServletConfig config)
            throws ServletException {

        SecurityCommons sc = new SecurityCommons();

        ctx = config.getServletContext();
        ctx.log("Starting (Filtered) UserCertServlet");

    /*
     * Cruft alert Need to initialise serial number to a known number and
     * persist between invocations
     *
     */

        bigSerial = BigInteger.valueOf(System.currentTimeMillis());

        Security.addProvider(new BouncyCastleProvider());

        getInitParams(config);

        if (paramsMissing == 0) {

            try {

                issuerKey = sc.readPrivateKey(issuerKeyPairFilename, issuerKeyPairPassword);

                if (issuerKey == null) {

                    ctx.log(String.format("Couldn't read %s.%n", issuerKeyPairFilename));
                    ctx.log(String.format("Check the passphrase or permissions on %s.%n", issuerKeyPairFilename));
                    throw new ServletException(
                            String.format("Couldn't read %s", issuerKeyPairFilename));

                }

                issuerCertificate = sc.getCertFromStream(new FileInputStream(issuerCertificateFilename));

                if (issuerCertificate == null) {

                    ctx.log(String.format("Couldn't read %s.%n", issuerCertificateFilename));
                    throw new ServletException(
                            String.format("Couldn't read %s", issuerCertificateFilename));

                }
                else {

                    issuerName = issuerCertificate.getSubjectDN().getName();
                    issuerName = StringUtils.replace(issuerName, "\"", "");
                    issuerName = sc.reverse(issuerName, ",");

                    if (debug) {
                        ctx.log(String.format("Issuer name = %s.", issuerName));
                    }

                    // TODO: check notAfter data of CA cert

                }

//        daysString = config.getInitParameter("certLifetimeDays");

                hoursString = config.getInitParameter("certLifetimeHours");


                if (hoursString == null) {
                    hours = DEFAULT_LIFETIME_HOURS;
                }
                else {
                    hours = Integer.valueOf(hoursString);
                }


                if (debug) {
                    ctx.log(String.format("Certificate lifetime = %d hours.", hours));
                }


//        PersistenceUtils.createInstance("appPU");

            }
            catch (MalformedURLException ex) {
                ctx.log(ex.getLocalizedMessage());
            }
            catch (CertificateException ex) {
                ctx.log(ex.getLocalizedMessage());
            }
            catch (IOException ex) {
                ctx.log(ex.getLocalizedMessage());
            }
            catch (NoSuchAlgorithmException ex) {

                ctx.log("Fatal: Cannot find algorithm to read key pair. Check location of BouncyCastle JARs");

            }
            catch (NullPointerException ex) {

                ctx.log("NPE");
                ex.printStackTrace();
                ctx.log(ex.getLocalizedMessage());
            }


        }
        else {

            ctx.log(String.format("Missing %d parameter values", paramsMissing));

        }

    }
}

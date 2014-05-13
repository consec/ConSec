/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.contrail.security;

import eu.contrail.security.servercommons.SAML;
import eu.contrail.security.servercommons.UserSAML;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;

import javax.security.auth.x500.X500Principal;
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

/**
 * @author ijj
 */
public class UserCertServlet extends HttpServlet {

    private static final long serialVersionUID = -1L;
    private static boolean debug = true;
    private static final int DEFAULT_LIFETIME_DAYS = 365;
    private static final int DEFAULT_LIFETIME_HOURS = 6;
    private static String daysString;
    private static String hoursString;
    private static int days;
    private static int hours;
    private static ServletContext ctx;
    private static PrivateKey issuerKey;
    private static String issuerKeyPairFilename;
    private static char[] issuerKeyPairPassword;
    private static X509Certificate issuerCertificate;
    private static String issuerCertificateFilename;
    private static String issuerName;
    private static BigInteger bigSerial;

    private PKCS10CertificationRequest getCSR(final HttpServletRequest request, SecurityCommons sc)
            throws IOException {

        PKCS10CertificationRequest csr = null;
        final String CSR_HEADER_NAME = "certificate_request";
        final String certificate_request = request.getParameter(CSR_HEADER_NAME);

        if (certificate_request == null) {
            if (debug) {
                ctx.log(String.format("UCS: Request Parameter %s is NULL", CSR_HEADER_NAME));
            }
        }
        else {
            final int csr_length = certificate_request.length();
            if (csr_length != 0) {
                csr = sc.readCSR(new ByteArrayInputStream(certificate_request.getBytes("UTF-8")));
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
        final SecurityCommons sc = new SecurityCommons();
        String pathInfo = request.getPathInfo();
        if (debug) {
            ctx.log(String.format("processing request", pathInfo));
            if (pathInfo != null && !"".equals(pathInfo.trim())) {
                ctx.log(String.format("UCS: PathInfo = %s.", pathInfo));
            }
        }
        final User user = (User) request.getAttribute("user");

	  /*
       * Sanity check - username must not be null
	   *
	   */
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            ctx.log("UCS: User object attribute is NULL - should have been set by BasicAuthFilter");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing authorization");
            return;
        }
        else {
            if (debug) {
                ctx.log(String.format("UCS: Username %s", user.getUsername()));
            }
        }
        //final int userID = user.getUserId();
        final String userUUID = user.getUuid();
        final String uuid = user.getUuid();
        ctx.log("UCS: UUID " + uuid);
        PrintWriter out = null;
        try {
            final PKCS10CertificationRequest certRequest = getCSR(request, sc);
            if (certRequest == null) {
                if (debug) {
                    ctx.log("UCS: CSR == NULL");
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

                final String errMsg = "UCS: Cannot verify CSR - InvalidKeyException - ignoring request";
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;

            }
            catch (SignatureException ex) {
                final String errMsg = "UCS: Cannot verify CSR - SignatureException - ignoring request";
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;
            }
            catch (Exception ex) {
                final String errMsg = "UCS: " + ex.getLocalizedMessage();
                ctx.log(errMsg);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;
            }
            X509Certificate cert;
            final int minutes = 0;
            if (debug) {
                ctx.log("UCS: about to createUserCert ");
            }
            UserSAML userSAML = new UserSAML();
            SAML saml = new SAML();
            String xml = userSAML.getSAMLforUser(user, saml);
            final String subjectName = String.format("CN=%s, CN=%s", userUUID, user.getUsername());
            final X500Principal subjectPrincipal = new X500Principal(issuerName + "," + subjectName);
            cert = sc.createUserCertificateWithSAML(
                    certRequest.getPublicKey(),
                    //        new X500Principal(String.format("CN=%d, CN=%s", userID, user.getUsername())), uuid,
                    subjectPrincipal,
                    uuid,
                    bigSerial,
                    issuerCertificate,
                    issuerKey,
                    "SHA1withRSA", // TODO: This constant should be a parameter read at startup time
                    days, hours, minutes,
                    SecurityCommons.CONTRAIL_ATTRIBUTE_ASSERTION, false, xml);

            if (cert == null) {
                if (debug) {
                    ctx.log("UCS: generated ertificate is NULL");
                }
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create a certificate");
                return;
            }
            if (debug) {
                ctx.log("UCS: About to write cert");
            }

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            sc.writeCertificate(new OutputStreamWriter(baos, "UTF-8"), cert);

            final String certString = baos.toString("UTF-8");

            response.setContentType("text/plain");
            response.setContentLength(certString.length()); // TO-DO: Try not setting content-length
            // In combination with not testing for is.available == 0 in client
            out = response.getWriter();
            out.write(certString);
            Object lock = new Object();
            synchronized (lock) {
                bigSerial = bigSerial.add(BigInteger.ONE);
            }
        }
        catch (CertificateException ex) {
            ctx.log(ex.getLocalizedMessage());
            throw new ServletException(ex.getLocalizedMessage());
        }
        catch (IOException ex) {
            ctx.log(ex.getLocalizedMessage());
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
        finally {
            if (out != null) {
                out.close();
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

    private static int paramsMissing = 0;

    private static void getInitParams(final ServletConfig config) {
        debug = Boolean.valueOf(config.getInitParameter("debug"));
        ctx.log(String.format("DEBUG set to %s", debug));
        issuerKeyPairFilename = config.getInitParameter("issuerKeyPairFilename");
        if (issuerKeyPairFilename != null) {
            if (debug) {
                ctx.log(String.format("UCS: issuerKeyPairFilename = %s.", issuerKeyPairFilename));
            }
        }
        else {
            paramsMissing++;
            ctx.log("UCS: Cannot read property issuerKeyPairFilename.");
        }
        final String keyPass = config.getInitParameter("issuerKeyPairPassword");
        if (keyPass != null) {
            issuerKeyPairPassword = keyPass.toCharArray();
        }
        else {
            ctx.log("UCS: Cannot read property issuerKeyPairPassword.");
          /*
           * Don't increment paramsMissing - this parameter is deprecated
		   * and need not be present
		   */
        }
        if (debug) {
            ctx.log(String.format("UCS: issuerKeyPairPassword is %s",
                    issuerKeyPairPassword == null ? "not set." : "set, but not logged here."));
        }
        issuerCertificateFilename = config.getInitParameter("issuerCertificateFilename");
        if (issuerCertificateFilename != null) {
            if (debug) {
                ctx.log(String.format("UCS: issuerCertificateFilename = %s.", issuerCertificateFilename));
            }
        }
        else {
            paramsMissing++;
            ctx.log("UCS: Cannot read property issuerCertificateFilename.");
        }
        hoursString = config.getInitParameter("certLifetimeHours");
        if (hoursString == null) {
            hours = -1;
        }
        else {
            try {
                hours = Integer.valueOf(hoursString);
            }
            catch (NumberFormatException ex) {
                hours = -1;
            }
        }
        daysString = config.getInitParameter("certLifetimeDays");
        if (daysString == null) {
            days = -1;
        }
        else {
            try {
                days = Integer.valueOf(daysString);
            }
            catch (NumberFormatException ex) {
                days = -1;
            }
        }

        if (days <= 0 || hours <= 0) {

		  /*
           * For user certificates obtained by CLI,
		   * default is to create a long-lived certificate
		   * 
		   */

            hours = 0;
            days = DEFAULT_LIFETIME_DAYS;
        }
    }

    @Override
    public void init(final ServletConfig config)
            throws ServletException {
        super.init(config);
        final SecurityCommons sc = new SecurityCommons();
        ctx = config.getServletContext();
        ctx.log("UCS: Starting");
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
                File f = new File(issuerKeyPairFilename);
                if (!f.canRead()) {
                    ctx.log(String.format("UCS: File permissions on %s do not allow reading.", issuerKeyPairFilename));
                }
                issuerKey = sc.readPrivateKey(issuerKeyPairFilename, issuerKeyPairPassword);
                if (issuerKey == null) {

                    ctx.log(String.format("UCS: Couldn't read %s.%n", issuerKeyPairFilename));
                    ctx.log(String.format("UCS: Check the passphrase or permissions on %s.%n", issuerKeyPairFilename));
                    throw new ServletException(
                            String.format("UCS: Couldn't read %s", issuerKeyPairFilename));

                }
                else {

                    ctx.log(String.format("UCS: Parsed Key OK - format %s.", issuerKey.getAlgorithm()));

                }

                issuerCertificate = sc.getCertFromStream(new FileInputStream(issuerCertificateFilename));

                if (issuerCertificate == null) {

                    System.err.print("UCS: Can't read cert");

                    ctx.log(String.format("UCS: Couldn't read %s.%n", issuerCertificateFilename));
                    throw new ServletException(
                            String.format("UCS: Couldn't read %s", issuerCertificateFilename));

                }
                else {

                    issuerName = issuerCertificate.getSubjectDN().getName();
                    //         issuerName = StringUtils.replace(issuerName, "\"", "");
                    issuerName = sc.reverse(issuerName, ",");

                    if (debug) {
                        ctx.log(String.format("UCS: Issuer name = %s.", issuerName));
                    }

                    // TODO: check notAfter data of CA cert

                }

                if (debug) {
                    ctx.log(String.format("UCS: Certificate lifetime = %d days, %d hours", days, hours));
                }

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

                ctx.log("UCS: Fatal: Cannot find algorithm to read key pair. Check location of BouncyCastle JARs");

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

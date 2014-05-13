/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.contrail.security;

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;

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
import java.util.UUID;

//import org.ow2.contrail.federation.federationdb.jpa.entities.User;

//import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

/**
 * @author ijj
 */
public class DelegatedHostCertServlet extends HttpServlet {

    private static final long serialVersionUID = -1L; // Stop FindBugs complaining

    /*
     * Static variables only set in threadsafe 'init' method
     *
     */
    private static boolean debug = false;
    private static String daysString;
    private static int days;
    private static final int DEFAULT_LIFETIME_HOURS = 12;
    private static String hoursString;
    private static int hours;
    private static int minutes = 0;
    private static ServletContext ctx;
    private static KeyPair issuerKeyPair;
    private static String issuerKeyPairFilename;
    private static char[] issuerKeyPairPassword;
    private static X509Certificate issuerCertificate;
    private static String issuerCertificateFilename;

    private static String issuerName;

    private static String allowedCNs;
    /*
     * Variable serialNumber is protected from concurrent update
     * by a synchronized block
     *
     */
    private static BigInteger serialNumber;


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
                csr = sc.readCSR(new ByteArrayInputStream(certificate_request.getBytes("UTF-8")));
            }

        }

        return csr;

    }

//  protected boolean authorise(final X509Certificate[] certs, final String allowedCNs) {
//
//    boolean authorised = false;
//
//    if (allowedCNs == null || allowedCNs.length() == 0) {
//      authorised = true;
//
//    } else {
//      final String commonName = certs[0].getSubjectDN().getName();
//
//      if (allowedCNs.indexOf(commonName) > -1) {
//        authorised = true;
//      }
//    }
//    return authorised;
//
//  }

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
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        SecurityCommons sc = new SecurityCommons();

        String logMessage = null;

//    EntityManager em = null;

        final String remoteAddr = request.getRemoteAddr();

        if (remoteAddr != null) {
            ctx.log("Request from IP " + remoteAddr);
            logMessage = "IP=" + remoteAddr;
        }

        PrintWriter out = response.getWriter();

        try {

            response.setContentType("text/plain");


      /*
       * Using Client Cert Authentication, retrieve list of client certificates
       *
       *
       */

            X509Certificate x509certificates[] =
                    (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

            if (x509certificates != null && x509certificates.length != 0) {

                if (debug) {

                    int i = 0;
                    for (X509Certificate c : x509certificates) {
                        ctx.log(String.format("Certificate %d: %s.%n", i, sc.reverse(c.getSubjectDN().getName(), ",")));
                        i++;
                    }

                }

            }
            else {

                ctx.log("Can't find certs in javax.servlet.request.X509Certificate");

                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;

            }
      /*
       *
       *
       */

            if (!sc.authorise(x509certificates, allowedCNs)) {

                ctx.log(logMessage + " - can't find authorised certificate");

                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;

            }


            final PKCS10CertificationRequest certRequest = getCSR(request, sc);

            if (certRequest == null) {

                final String errMsg = "The request is missing the parameter 'certificate_request'";

                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;

            }

            // TODO: check length of request public key - define allowable lengths
            // TODO: check key type - only allow RSA?

            try {
                certRequest.verify();
            }
            catch (InvalidKeyException ex) {

                final String errMsg = "Cannot verify CSR - InvalidKeyException - ignoring request";
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;
            }
            catch (SignatureException ex) {

                final String errMsg = "Cannot verify CSR - SignatureException - ignoring request";
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;
            }


      /*
       * Extract hostname from CSR Subject field CN
       *
       */

            String subject = certRequest.getCertificationRequestInfo().getSubject().toString();

            if (subject == null) {

                final String errMsg = "Client didn't send Subject DN";
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;
            }

            UUID uuid;

            String hostname;


            String[] rdnArr = null;

            rdnArr = sc.getRDNs(subject, BCStyle.CN);


            if (rdnArr == null) {

                final String errMsg = "Client didn't send Subject hostname in CN field";
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;

            }
            else {

                hostname = rdnArr[0];

            }


            if (!sc.isValidFQDN(hostname)) {

                final String errMsg = String.format("Client sent badly formatted Subject CN=hostname: %s", hostname);
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;

            }


            if (debug) {
                ctx.log(String.format("DHCS: hostname from CSR = %s", hostname));
            }

            logMessage = logMessage + ", hostname=" + hostname;



      /*
       * Create the host certificate given the FQDN
       * 
       * 
       * 
       */

            issuerCertificate.getSubjectDN().getName();

            X509Certificate cert = sc.createHostCertificate(
                    certRequest.getPublicKey(),
                    hostname,
                    serialNumber,
                    issuerCertificate,
                    issuerKeyPair.getPrivate(),
                    "SHA1withRSA", new Date(),
                    days, hours, minutes);


            if (cert == null) {

                if (debug) {
                    ctx.log("createHostCertificate returned NULL");
                }

                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create a certificate");
                return;
            }


            if (debug) {
                ctx.log("About to write cert");
            }

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            sc.writeCertificate(new OutputStreamWriter(baos, "UTF-8"), cert);

            final String certString = baos.toString("UTF-8");

            response.setContentType("text/plain");
            response.setContentLength(certString.length()); // TO-DO: Try not setting content-length
            // In combination with not testing for is.available == 0 in client

            out = response.getWriter();
            out.write(certString);

            if (debug) {
                ctx.log("Wrote cert");
            }

            logMessage = logMessage + ", serial=" + cert.getSerialNumber();

            ctx.log(logMessage);

            Object lock = new Object();

            synchronized (lock) {
                serialNumber = serialNumber.add(BigInteger.ONE);  // Handle multiple servlet instances with synchronized
            }

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


            throw new ServletException(ex.getLocalizedMessage());
        }
        finally {

//      PersistenceUtils.getInstance().closeEntityManager(em);

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
        processRequest(request, response);
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
        return "Short description";
    }// </editor-fold>

    private static int paramsMissing = 0;

    // TODO: Move the functionality below into init(), mostly to make Findbugs shut up

    private void getInitParams(final ServletConfig config) {


        debug = Boolean.valueOf(config.getInitParameter("debug"));

        allowedCNs = config.getInitParameter("allowedCNs");

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

    }

    @Override
    public void init(final ServletConfig config)
            throws ServletException {

        SecurityCommons sc = new SecurityCommons();

        ctx = config.getServletContext();
        ctx.log("Starting DelegatedHostCertServlet");

    /*
     * Cruft alert Need to initialise serial number to a known number and
     * persist between invocations
     *
     */

        serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        Security.addProvider(new BouncyCastleProvider());

        getInitParams(config);

        InputStream is = null;

        if (paramsMissing == 0) {

            try {

                if (!new File(issuerKeyPairFilename).canRead()) {
                    ctx.log(String.format("Fatal - can't read key %s", issuerKeyPairFilename));
                }


                issuerKeyPair = sc.readKeyPair(issuerKeyPairFilename, issuerKeyPairPassword);

                if (issuerKeyPair == null) {

                    ctx.log(String.format("Couldn't read %s.%n", issuerKeyPairFilename));
                    ctx.log(String.format("Check the passphrase or permissions on %s.%n", issuerKeyPairFilename));
                    throw new ServletException(
                            String.format("Couldn't read %s", issuerKeyPairFilename));

                }

                if (!new File(issuerCertificateFilename).canRead()) {
                    ctx.log(String.format("Fatal - can't read certificate %s", issuerCertificateFilename));
                }

                is = new FileInputStream(issuerCertificateFilename);
                issuerCertificate = sc.getCertFromStream(is);

                is.close();

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

                daysString = config.getInitParameter("certLifetimeDays");

                if (daysString == null) {
                    days = -1;
                }
                else {
                    days = Integer.valueOf(daysString);
                    days = Math.max(days, 0);

                    if (debug) {
                        if (days != 0) {
                            ctx.log(String.format("Cert lifetime is %d days.%n", days));
                        }
                    }
                }


                hoursString = config.getInitParameter("certLifetimeHours");

                if (hoursString == null) {
                    hours = -1;
                }
                else {
                    hours = Integer.valueOf(hoursString);
                    hours = Math.max(hours, 0);

                    if (debug) {
                        ctx.log(String.format("Cert lifetime is %d hours.%n", hours));
                    }

                }


                if (days == 0 && hours == 0) {
                    days = 0;
                    hours = DEFAULT_LIFETIME_HOURS;

                    if (debug) {
                        ctx.log(String.format("No certificate lifetime parameters set - using default of %s hours.", DEFAULT_LIFETIME_HOURS));
                    }

                }

//        PersistenceUtils pu = PersistenceUtils.getInstance();
//
//        if (pu == null) {
//          PersistenceUtils.createInstance("appPU"); // Ideally, should be a getinstance call which does 
//          // createInstance if no instance constructed yet
//        }

            }
            catch (MalformedURLException ex) {

                ctx.log(ex.getLocalizedMessage());

            }
            catch (CertificateException ex) {

                ctx.log(ex.getLocalizedMessage());

            }
            catch (IOException ex) {

                if (is != null) {

                    try {
                        is.close();
                    }
                    catch (IOException exx) {
                        ;
                    }
                }

                ctx.log(ex.getLocalizedMessage());

            }
            catch (NoSuchAlgorithmException ex) {

                ctx.log("Fatal: Cannot find algorithm to read key pair. Check location of BouncyCastle JARs");

            }
            catch (NullPointerException ex) {
                ex.printStackTrace();
                ctx.log(ex.getLocalizedMessage());
            }


        }
        else {

            ctx.log(String.format("Missing %d parameter values", paramsMissing));

        }

    }
}

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
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
public class DelegatedUserCertServlet extends HttpServlet {

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
    private static PrivateKey issuerKey;
    private static String issuerKeyPairFilename;
    private static char[] issuerKeyPairPassword = null;
    ;
    private static X509Certificate issuerCertificate;
    private static String issuerCertificateFilename;
    private static String allowedCNs;
    private static String issuerName;
    /*
     * Variable serialNumber is protected from concurrent update
     * by a synchronized block
     *
     */
    private static BigInteger serialNumber;
    private static EntityManager em = null;

    private PKCS10CertificationRequest getCSR(final HttpServletRequest request, SecurityCommons sc)
            throws IOException {

        PKCS10CertificationRequest csr = null;

        final String CSR_HEADER_NAME = "certificate_request";

        final String certificate_request = request.getParameter(CSR_HEADER_NAME);

        if (certificate_request == null) {

            if (debug) {
                ctx.log(String.format("DUCS: Request Parameter %s is NULL", CSR_HEADER_NAME));
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

//  protected String getUserID(final String subject, SecurityCommons sc) {
//
//    String userID = null;
//
//    String[] rdnArr = null;
//
//    rdnArr = sc.getRDNs(subject, BCStyle.CN);
//
//    /*
//     * This should change to finding the UUID (or either userID or UUID)
//     * from the Subject 
//     * 
//     * 
//     * 
//     */
//
//    if (rdnArr != null) {
//
//      for (String dn : rdnArr) {
//
//        if (sc.isUserId(dn)) {
//          userID = dn;
//          break;
//        }
//      }
//
//    }
//    return userID;
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

        SAML saml = new SAML();

        String logMessage = null;


        final String remoteAddr = request.getRemoteAddr();

        if (remoteAddr != null) {
            ctx.log("DUCS: Request from IP " + remoteAddr);
            logMessage = "DUCS: IP=" + remoteAddr;
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
                        //ctx.log(String.format("Certificate %d: %s.%n", i, sc.reverse(c.getSubjectDN().getName(), ",")));
                        i++;
                    }

                }

            }
            else {

                ctx.log("DUCS: Can't find certs in javax.servlet.request.X509Certificate");

                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;

            }
      /*
       * Check the client certificate against the list of allowed CNs
       *
       */

            if (!sc.authorise(x509certificates, allowedCNs)) {

                ctx.log(logMessage + " - can't find authorised certificate");

                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;

            }


            final PKCS10CertificationRequest certRequest = getCSR(request, sc);

            if (certRequest == null) {

                final String errMsg = "DUCS: The request is missing the parameter 'certificate_request'";

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

                final String errMsg = "DUCS: Cannot verify CSR - InvalidKeyException - ignoring request";
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;
            }
            catch (SignatureException ex) {

                final String errMsg = "DUCS: Cannot verify CSR - SignatureException - ignoring request";
                if (debug) {
                    ctx.log(errMsg);
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
                return;
            }


            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || authHeader.length() == 0 || "".equals(authHeader)) {

                response.setHeader("WWW-Authenticate", "Basic realm=\"my-contrail-onlineca-realm\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

                if (debug) {
                    ctx.log("DUCS: Sending BasicAuth challenge");
                }
                return;

            }

            final String[] usernameAndPassword = sc.getBasicAuthUsernamePassword(authHeader);

            ctx.log(String.format("DUCS: userID = %s", usernameAndPassword[0]));

            String userId = usernameAndPassword[0];

            if (debug) {
                logMessage = logMessage + ", UserID from BasicAuth header = " + userId;
            }


      /*
       * TODO: Can we obtain the EntityManager just once, e.g. when
       * the servlet is started?
       * 
       */

//      em = PersistenceUtils.getInstance().getEntityManager();

            if (em == null) {

                if (debug) {

                    ctx.log("DUCS: EntityManager is NULL");

                }

                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;

            }

            UserSAML userSAML = new UserSAML();

            User user = null;

            boolean badMatch = false;

            try {

                user = userSAML.getUserbyUserID(em, userId);

            }
            catch (NumberFormatException ex) {

                if (debug) {
                    ctx.log(String.format("DUCS: UserID %s is not valid", userId));
                }

            }
            catch (NoResultException ex) {

                if (debug) {
                    ctx.log(String.format("DUCS: No user found for ID %s", userId));
                }

            }

            if (user == null) {

                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;

            }


//      TypedQuery<User> query = em.createNamedQuery("User.findByUserId", User.class);
//
//
//      if (query == null) {
//
//        if (debug) {
//          ctx.log("Couldn't create Typedquery");
//        }
//
//        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//        return;
//
//      }
//
//      /*
//       * Retrieve a User object corresponing to the passed-in userID
//       * 
//       */
//
//
//      if (debug) {
//        ctx.log("query.setParameter");
//      }
//
//
//      String uuid = null;
//
//      String username = null;
//
//
//      try {
//
//        query.setParameter("userId", Integer.valueOf(userId));
//
//        user = query.getSingleResult();
//
//        if (user == null) {
//
//          if (debug) {
//            ctx.log("User object from query.getSingleResult is NULL");
//          }
//          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//          return;
//
//        }
//
//        uuid = user.getUuid();
//        username = user.getUsername();
//
//      } catch (NumberFormatException ex) {
//
//        if (debug) {
//          ctx.log(String.format("Non-numeric UserID passed - %s.", userId));
//        }
//        badMatch = true;
//
//      } catch (NoResultException ex) {
//
//        if (debug) {
//          ctx.log(String.format("No entry in database for %s.", userId));
//        }
//        badMatch = true;
//
//      } catch (NonUniqueResultException ex) {
//
//        if (debug) {
//          ctx.log(String.format("Multiple entries in database for %s.", userId));
//        }
//        badMatch = true;
//      }
//
//
//
//      if (badMatch) {
//
//        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//        return;
//
//      }

      /*
       * Create the user certificate from the User object
       * 
       * 
       * 
       */

            String username = user.getUsername();
            if (debug) {
                ctx.log("DUCS: username " + username);
            }
            String uuid = user.getUuid();
            if (debug) {
                ctx.log("DUCS: uuid " + uuid);
            }
            String xml = userSAML.getSAMLforUser(user, saml);
            final String subjectName = String.format("CN=%s, CN=%s", uuid, username);
            if (debug) {
                ctx.log("DUCS: About to write cert with subject name " + subjectName);
            }
            final X500Principal subjectPrincipal = new X500Principal(issuerName + "," + subjectName);
            final String certSigAlg = "SHA1withRSA";  // Will be made an init-parameter
            X509Certificate cert = sc.createUserCertificateWithSAML(
                    certRequest.getPublicKey(),
                    subjectPrincipal,
                    uuid,
                    serialNumber,
                    issuerCertificate,
                    issuerKey,
                    certSigAlg,
                    days, hours, minutes,
                    SecurityCommons.CONTRAIL_ATTRIBUTE_ASSERTION, false, xml);
            if (cert == null) {

                if (debug) {
                    ctx.log("DUCS: createCertificate returned NULL");
                }

                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create a certificate");
                return;
            }


            if (debug) {
                ctx.log("DUCS: About to write cert");
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
                ctx.log("DUCS: Wrote cert");
            }

            logMessage = logMessage + ", serial=" + cert.getSerialNumber();

            ctx.log(logMessage);

            Object lock = new Object();

            synchronized (lock) {
                serialNumber = serialNumber.add(BigInteger.ONE);  // Handle multiple servlet instances
            }

        }
        catch (IllegalArgumentException ex) {

            logMessage = ex.getLocalizedMessage();

            response.sendError(HttpServletResponse.SC_BAD_REQUEST, logMessage);
            throw new ServletException(logMessage);

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

      /*
       * Don't close the EntitityManager
       */
//      PersistenceUtils.getInstance().closeEntityManager(em);

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

    private static void getInitParams(final ServletConfig config) {


        debug = Boolean.valueOf(config.getInitParameter("debug"));
        ctx.log("DEBUG is set to " + debug);
        allowedCNs = config.getInitParameter("allowedCNs");

        issuerKeyPairFilename = config.getInitParameter("issuerKeyPairFilename");

        if (issuerKeyPairFilename != null) {
            if (debug) {
                ctx.log(String.format("DUCS: issuerKeyPairFilename = %s.", issuerKeyPairFilename));
            }

        }
        else {

            paramsMissing++;
            ctx.log("DUCS: Cannot read property issuerKeyPairFilename.");

        }

        final String keyPass = config.getInitParameter("issuerKeyPairPassword");

        if (keyPass != null && !keyPass.equals("")) {

            issuerKeyPairPassword = keyPass.toCharArray();

        }
        else {

            ctx.log("DUCS: Cannot read property issuerKeyPairPassword.");
      /*
       * Don't incremenet paramsMissing as it is deprecated
       * and need not be set
       */

        }

        if (debug) {

            ctx.log(String.format("DUCS: issuerKeyPairPassword is %s",
                    issuerKeyPairPassword == null ? "not set." : "set, but not logged here."));

        }

        issuerCertificateFilename = config.getInitParameter("issuerCertificateFilename");

        if (issuerCertificateFilename != null) {

            if (debug) {

                ctx.log(String.format("DUCS: issuerCertificateFilename = %s.", issuerCertificateFilename));
            }

        }
        else {

            paramsMissing++;
            ctx.log("DUCS: Cannot read property issuerCertificateFilename.");

        }

    }

    @Override
    public void init(final ServletConfig config)
            throws ServletException {
        super.init(config);
        SecurityCommons sc = new SecurityCommons();

        ctx = config.getServletContext();
        ctx.log("DUCS: Starting");

    /*
     * Cruft alert Need to initialise serial number to a known number and
     * persist between invocations
     *
     */

        serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        Security.addProvider(new BouncyCastleProvider());

        getInitParams(config);

        if (paramsMissing == 0) {

            try {

                File f = new File(issuerKeyPairFilename);

                FileInputStream in = null;
                if (!f.canRead()) {
                    ctx.log(String.format("DUCS: Fatal - can't read key %s", issuerKeyPairFilename));
                }
                else {
                    in = new FileInputStream(issuerKeyPairFilename);
                }

                issuerKey = sc.readPrivateKey(in, issuerKeyPairPassword);

                if (issuerKey == null) {

                    ctx.log(String.format("DUCS: Couldn't read %s.%n", issuerKeyPairFilename));
                    ctx.log(String.format("DUCS: Check the passphrase or permissions on %s.%n", issuerKeyPairFilename));
                    throw new ServletException(
                            String.format("DUCS: Couldn't read %s", issuerKeyPairFilename));

                }
                else {

                    ctx.log(String.format("DUCS: Parsed Key OK - format %s.", issuerKey.getAlgorithm()));

                }


                if (!new File(issuerCertificateFilename).canRead()) {
                    ctx.log(String.format("DUCS: Fatal - can't read certificate %s", issuerCertificateFilename));
                }

                issuerCertificate = sc.getCertFromStream(new FileInputStream(issuerCertificateFilename));

                if (issuerCertificate == null) {

                    ctx.log(String.format("DUCS: Couldn't read %s.%n", issuerCertificateFilename));
                    throw new ServletException(
                            String.format("DUCS: Couldn't read %s", issuerCertificateFilename));

                }
                else {

                    issuerName = issuerCertificate.getSubjectDN().getName();
//          issuerName = StringUtils.replace(issuerName, "\"", "");
                    issuerName = sc.reverse(issuerName, ",");

                    if (debug) {
                        ctx.log(String.format("DUCS: Issuer name = %s.", issuerName));
                    }

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
                            ctx.log(String.format("DUCS: Cert lifetime is %d days.%n", days));
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
                        ctx.log(String.format("DUCS: Cert lifetime is %d hours.%n", hours));
                    }

                }


                if (days == 0 && hours == 0) {
                    days = 0;
                    hours = DEFAULT_LIFETIME_HOURS;

                    if (debug) {
                        ctx.log(String.format("DUCS: No certificate lifetime parameters set - using default of %s hours.", DEFAULT_LIFETIME_HOURS));
                    }

                }

                PersistenceUtils pu = PersistenceUtils.getInstance();

                if (pu == null) {
                    pu = PersistenceUtils.createInstance("appPU"); // Ideally, should be a getinstance call which does
                    // createInstance if no instance constructed yet
                }

                em = pu.getEntityManager();

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

                ctx.log("DUCS: Fatal: Cannot find algorithm to read key pair. Check location of BouncyCastle JARs");

            }
            catch (NullPointerException ex) {
                ex.printStackTrace();
                ctx.log(ex.getLocalizedMessage());
            }


        }
        else {

            ctx.log(String.format("DUCS: Missing %d parameter values", paramsMissing));

        }

    }
}

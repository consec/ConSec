/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.contrail.security;

import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.cert.X509Certificate;

/**
 * @author ijj
 */


public class UserCertSSLAuthFilter implements Filter {

    private static ServletContext ctx;


    private static boolean debug = false;
    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured.
    private FilterConfig filterConfig = null;

    private static String allowedCNs;


    public UserCertSSLAuthFilter() {
    }

    private boolean doBeforeProcessing(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {


        if (debug) {
            log("BasicAuthFilter:DoBeforeProcessing");
        }

        // Write code here to process the request and/or response before
        // the rest of the filter chain is invoked.

        // For example, a logging filter might log items on the request object,
        // such as the parameters.


        HttpServletRequest httpRequest = (HttpServletRequest) request;

        HttpServletResponse httpResponse = (HttpServletResponse) response;


        if ("GET".equalsIgnoreCase(httpRequest.getMethod())) {
            if (debug) {
                log("Rejecting GET method");
            }
            httpResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return false;  // This signals calling code - doFilter - to not carry on
            // and the servlet isn't called
      
      /*
       * Ideally, want to just return response to client
       * Rather than call next entry in filter chain (i.e. the servlet)
       * 
       */
        }

        final String remoteAddr = httpRequest.getRemoteAddr();

        if (remoteAddr != null) {
            if (debug) {
                log("BasicAuthFilter: Request from IP " + remoteAddr);
            }
        }


        final String[] usernameAndPassword = SecurityUtils.
                getBasicAuthUsernamePassword(httpRequest.getHeader("Authorization"));

        String username = null; //NOPMD
        String password = null; //NOPMD


        if (usernameAndPassword == null) {

            final String msg = "Cannot retrieve username and password from Authorization header";

            if (debug) {
                ctx.log(msg);

            }

            System.err.println(msg);

//      httpResponse.setHeader("WWW-Authenticate", "BASIC realm=\"Contrail\"");
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, msg); // , "Cannot retrieve username and password from Authorization header");
            return false;

        }

        username = usernameAndPassword[0];
        password = usernameAndPassword[1];

        if (debug) {
            ctx.log(String.format("BasciAuthFilter: Username=%s.", username));
        }

        User user = null;
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        if (em == null) {

            if (debug) {

                ctx.log("Couldn't create EntityManager");

            }

            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;

        }


        Query query = em.createQuery("SELECT u FROM User u WHERE u.username = :username AND u.password = :password");

        if (query == null) {

            if (debug) {

                ctx.log("Couldn't create Query");

            }

            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;

        }

        query.setParameter("username", username);
        query.setParameter("password", password);

        boolean badMatch = false;
        try {

            user = (User) query.getSingleResult();

            if (user == null) {

                if (debug) {

                    ctx.log("User object from query.getSingleResult is NULL");

                }

                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return false;

            }

        }
        catch (NoResultException ex) {

            if (debug) {
                ctx.log(String.format("No entry in database for %s.", username));
            }
            badMatch = true;

        }
        catch (NonUniqueResultException ex) {

            if (debug) {
                ctx.log(String.format("Multiple entries in database for %s.", username));
            }
            badMatch = true;
        }


//      if ("validuser".equals(username) && "validpassword".equals(password)) {
//        
//        if (debug) {
//          log("BasicAuthFilter:Valid user");
//        }
//      
//        httpRequest.setAttribute("username", username);
//      
//      } else {

        if (badMatch) {

            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;

        }
        else {

            httpRequest.setAttribute("user", user);

        }


        return true;

    }

    private void doAfterProcessing(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
        if (debug) {
            log("BasicAuthFilter:DoAfterProcessing");
        }

        // Write code here to process the request and/or response after
        // the rest of the filter chain is invoked.

        // For example, a logging filter might log the attributes on the
        // request object after the request has been processed.
    /*
     * for (Enumeration en = request.getAttributeNames(); en.hasMoreElements();
     * ) { String name = (String)en.nextElement(); Object value =
     * request.getAttribute(name); log("attribute: " + name + "=" +
     * value.toString());
     *
     * }
     */

        // For example, a filter might append something to the response.
    /*
     * PrintWriter respOut = new PrintWriter(response.getWriter());
     * respOut.println("<P><B>This has been appended by an intrusive
     * filter.</B>");
     */
    }

    /**
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @param chain    The filter chain we are processing
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet error occurs
     */
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        if (debug) {
            log("BasicAuthFilter:doFilter()");
        }

        boolean carryOn = doBeforeProcessing(request, response);

        if (carryOn) {

            Throwable problem = null;
            try {
                chain.doFilter(request, response);
            }
            catch (Throwable t) {
                // If an exception is thrown somewhere down the filter chain,
                // we still want to execute our after processing, and then
                // rethrow the problem after that.
                problem = t;
                t.printStackTrace();
            }

            doAfterProcessing(request, response);

            // If there was a problem, we want to rethrow it if it is
            // a known type, otherwise log it.
            if (problem != null) {
                if (problem instanceof ServletException) {
                    throw (ServletException) problem;
                }
                if (problem instanceof IOException) {
                    throw (IOException) problem;
                }
                sendProcessingError(problem, response);
            }

        }

    }

    /**
     * Return the filter configuration object for this filter.
     */
    public FilterConfig getFilterConfig() {
        return (this.filterConfig);
    }

    /**
     * Set the filter configuration object for this filter.
     *
     * @param filterConfig The filter configuration object
     */
    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    /**
     * Destroy method for this filter
     */
    public void destroy() {
    }

    /**
     * Init method for this filter
     */
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        if (filterConfig != null) {

            ctx = filterConfig.getServletContext();

            allowedCNs = filterConfig.getInitParameter("allowedCNs");

            if (debug) {
                log("USerCertSSLAuthFilter:Initializing filter");
            }

        }

    }

    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {
        if (filterConfig == null) {
            return ("BasicAuthFilter()");
        }
        StringBuffer sb = new StringBuffer("BasicAuthFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());
    }

    private void sendProcessingError(Throwable t, ServletResponse response) {
        String stackTrace = getStackTrace(t);

        if (stackTrace != null && !stackTrace.equals("")) {
            try {
                response.setContentType("text/html");
                PrintStream ps = new PrintStream(response.getOutputStream());
                PrintWriter pw = new PrintWriter(ps);
                pw.print("<html>%n<head>%n<title>Error</title>%n</head>%n<body>%n"); //NOI18N

                // PENDING! Localize this for next official release
                pw.print("<h1>The resource did not process correctly</h1>%n<pre>%n");
                pw.print(stackTrace);
                pw.print("</pre></body>%n</html>"); //NOI18N
                pw.close();
                ps.close();
                response.getOutputStream().close();
            }
            catch (Exception ex) {
            }
        }
        else {
            try {
                PrintStream ps = new PrintStream(response.getOutputStream());
                t.printStackTrace(ps);
                ps.close();
                response.getOutputStream().close();
            }
            catch (Exception ex) {
            }
        }
    }

    public static String getStackTrace(Throwable t) {
        String stackTrace = null;
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            sw.close();
            stackTrace = sw.getBuffer().toString();
        }
        catch (Exception ex) {
        }
        return stackTrace;
    }

    public void log(String msg) {
        ctx.log(msg);
    }

    private static final long serialVersionUID = -1L; // Stop FindBugs complaining

  /*
   * Static variables only set in threadsafe 'init' method
   *  
   */


    private static String issuerName;


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
    private boolean doBeforeProcessing(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String logMessage = null;

        EntityManager em = null;

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
                        ctx.log(String.format("Certificate %d: %s.%n", i, SecurityUtils.reverse(c.getSubjectDN().getName(), ",")));
                        i++;
                    }

                }

            }
            else {

                ctx.log("Can't find certs in javax.servlet.request.X509Certificate");

                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return false;

            }
      /*
       * Check the client certificate against the list of allowed CNs
       *
       */

            if (!SecurityUtils.authorise(x509certificates, allowedCNs)) {

                ctx.log(logMessage + " - can't find authorised certificate");

                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return false;

            }

            return true;


        }
        catch (IOException ex) {

            ctx.log(ex.getLocalizedMessage());

            // throw new ServletException(ex.getLocalizedMessage());

        }
        finally {

            PersistenceUtils.getInstance().closeEntityManager(em);

            if (out != null) {

                try {
                    out.close();
                }
                catch (Exception ex) {
                    ;
                }
            }

            return true;

        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
//  @Override
//  protected void doGet(HttpServletRequest request, HttpServletResponse response)
//    throws ServletException, IOException {
//    processRequest(request, response);
//  }
//
//  /**
//   * Handles the HTTP
//   * <code>POST</code> method.
//   *
//   * @param request servlet request
//   * @param response servlet response
//   * @throws ServletException if a servlet-specific error occurs
//   * @throws IOException if an I/O error occurs
//   */
//  @Override
//  protected void doPost(HttpServletRequest request, HttpServletResponse response)
//    throws ServletException, IOException {
//    processRequest(request, response);
//  }
//
//  /**
//   * Returns a short description of the servlet.
//   *
//   * @return a String containing servlet description
//   */
//  @Override
//  public String getServletInfo() {
//    return "Short description";
//  }// </editor-fold>
    private static int paramsMissing = 0;

    private static void getInitParams(final ServletConfig config) {


        debug = Boolean.valueOf(config.getInitParameter("debug"));

        allowedCNs = config.getInitParameter("allowedCNs"); // Need to set a property
        // which points to a file containing allowedCNs


    }


//  public void init(final FilterConfig config)
//    {
//
//    ctx = config.getServletContext();
//    ctx.log("Starting DelegatedUserCertServlet");
//
//    /*
//     * Cruft alert Need to initialise serial number to a known number and
//     * persist between invocations
//     *
//     */
//
// 
//
//    Security.addProvider(new BouncyCastleProvider());
//
//    getInitParams(config);
//
//    if (paramsMissing == 0) {
//
//      try {
//
//        if (!new File(issuerKeyPairFilename).canRead()) {
//          ctx.log(String.format("Fatal - can't read key %s", issuerKeyPairFilename));
//        }
//
//
//        issuerKey = SecurityUtils.readPrivateKey(issuerKeyPairFilename, issuerKeyPairPassword);
//        
//        
//
//        if (issuerKey == null) {
//
//          ctx.log(String.format("Couldn't read %s.%n", issuerKeyPairFilename));
//          ctx.log(String.format("Check the passphrase or permissions on %s.%n", issuerKeyPairFilename));
//          throw new ServletException(
//            String.format("Couldn't read %s", issuerKeyPairFilename));
//
//        } else {
////          PublicKey pub = issuerKey.getPublic();
////          ctx.log(pub.getAlgorithm());
//        }
//
//
//
//        if (!new File(issuerCertificateFilename).canRead()) {
//          ctx.log(String.format("Fatal - can't read certificate %s", issuerCertificateFilename));
//        }
//
//        issuerCertificate = SecurityUtils.getCertFromStream(new FileInputStream(issuerCertificateFilename));
//
//        if (issuerCertificate == null) {
//
//          ctx.log(String.format("Couldn't read %s.%n", issuerCertificateFilename));
//          throw new ServletException(
//            String.format("Couldn't read %s", issuerCertificateFilename));
//
//        } else {
//          
//          issuerName = issuerCertificate.getSubjectDN().getName();     
//          issuerName = StringUtils.replace(issuerName, "\"", "");
//          issuerName = SecurityUtils.reverse(issuerName, ",");
//          
//          if (debug) {
//            ctx.log(String.format("Issuer name = %s.", issuerName));
//          }
//          
//        }
//
//        daysString = config.getInitParameter("certLifetimeDays");
//
//        if (daysString == null) {
//          days = -1;
//        } else {
//          days = Integer.valueOf(daysString);
//          days = Math.max(days, 0);
//
//          if (debug) {
//            if (days != 0) {
//              ctx.log(String.format("Cert lifetime is %d days.%n", days));
//            }
//          }
//        }
//
//
//        hoursString = config.getInitParameter("certLifetimeHours");
//
//        if (hoursString == null) {
//          hours = -1;
//        } else {
//          hours = Integer.valueOf(hoursString);
//          hours = Math.max(hours, 0);
//
//          if (debug) {
//            ctx.log(String.format("Cert lifetime is %d hours.%n", hours));
//          }
//
//        }
//
//
//        if (days == 0 && hours == 0) {
//          days = 0;
//          hours = DEFAULT_LIFETIME_HOURS;
//
//          if (debug) {
//            ctx.log(String.format("No certificate lifetime parameters set - using default of %s hours.", DEFAULT_LIFETIME_HOURS));
//          }
//
//        }
//
//        PersistenceUtils pu = PersistenceUtils.getInstance();
//
//        if (pu == null) {
//          PersistenceUtils.createInstance("appPU"); // Ideally, should be a getinstance call which does 
//          // createInstance if no instance constructed yet
//        }
//
//      } catch (MalformedURLException ex) {
//        ctx.log(ex.getLocalizedMessage());
//      } catch (CertificateException ex) {
//        ctx.log(ex.getLocalizedMessage());
//      } catch (IOException ex) {
//        ctx.log(ex.getLocalizedMessage());
//      } catch (NoSuchAlgorithmException ex) {
//
//        ctx.log("Fatal: Cannot find algorithm to read key pair. Check location of BouncyCastle JARs");
//
//      } catch (NullPointerException ex) {
//        ex.printStackTrace();
//        ctx.log(ex.getLocalizedMessage());
//      }
//
//
//    } else {
//
//      ctx.log(String.format("Missing %d parameter values", paramsMissing));
//
//    }
//
//  }
}

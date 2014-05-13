/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.contrail.security;

import org.mindrot.jbcrypt.BCrypt;
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

/**
 * @author ijj
 */
//@WebFilter(filterName = "BasicAuthFilter", servletNames = {"FilteredUserCertServlet"}, dispatcherTypes = {DispatcherType.REQUEST})
public class BasicAuthFilter implements Filter {


    private static ServletContext ctx;


    private static final boolean debug = true;
    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured.
    private FilterConfig filterConfig = null;

    private SecurityCommons sc = new SecurityCommons();

    private static PersistenceUtils pu = null;
    ;

    public BasicAuthFilter() {
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
                log("\nBasicAuthFilter: Request from IP " + remoteAddr);
            }
        }

        final String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader == null || authHeader.length() == 0 || "".equals(authHeader)) {

            httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"my-contrail-onlineca-realm\"");
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            if (debug) {
                log("Sending BasicAuth challenge");
            }
            return false;

        }


        final String[] usernameAndPassword = sc.getBasicAuthUsernamePassword(authHeader);

        String username = null; //NOPMD
        String password = null; //NOPMD


        if (usernameAndPassword == null || usernameAndPassword.length != 2) {

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

//    password = BCrypt.hashpw(password, BCrypt.gensalt(12));


        if (debug) {
            ctx.log(String.format("BasciAuthFilter: Username=%s.", username));
        }

        User user = null;
        EntityManager em = pu.getEntityManager();

        if (em == null) {

            if (debug) {

                ctx.log("Couldn't create EntityManager");

            }

            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;

        }


        String queryString;

//    queryString = "SELECT u FROM User u WHERE u.username = :username AND u.password = :password";

        queryString = "SELECT u FROM User u WHERE u.username = :username";


        Query query = em.createQuery(queryString);

        if (query == null) {

            if (debug) {

                ctx.log("Couldn't create Query");

            }

            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;

        }

        query.setParameter("username", username);
//    query.setParameter("password", password);

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

            badMatch = !BCrypt.checkpw(password, user.getPassword());

            if (badMatch) {

                if (debug) {
                    ctx.log(String.format("BCrypt.checkpw returns false"));
                }
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

            ctx.log(String.format("Username and password do not match"));

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


            if (debug) {
                log("BasicAuthFilter:Initializing filter");
            }

            pu = PersistenceUtils.getInstance();

            if (pu == null) {

                log("About to createInstance");

                pu = PersistenceUtils.createInstance("appPU"); // Ideally, should be a getinstance call which does
                // createInstance if no instance constructed yet

                // TODO - change name of persistence unit to read from config file
            }
            if (pu == null) {
                log(String.format("BAF: PU is NULL"));
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
}

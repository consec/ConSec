package org.ow2.contrail.federation.federationapi.utils;

import org.apache.log4j.Logger;
import org.ow2.contrail.federation.federationapi.MyServletContextListener;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Authorizer {

    protected static Logger logger =
            Logger.getLogger(Authorizer.class);

    /**
     * Reads user name from the request (client certificate) and checks whether
     * the user name is in the list given from the servlet context.
     *
     * @param request
     * @return
     */
    public static boolean isAuthorized(HttpServletRequest request) {
        if (request == null || !request.isSecure()) {
            return true;  // for testing purposes only
        }

        try {
            X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
            if (certs == null) {
                return false;
            }
            Principal principal = certs[0].getSubjectDN();
            String dn = principal.getName();

            Pattern dnPattern = Pattern.compile("CN=([\\w ]+),");
            Matcher m = dnPattern.matcher(dn);
            if (m.find()) {
                String username = m.group(1);
                if (MyServletContextListener.isAuthZEnabled()) {
                    if (MyServletContextListener.getAuthzList().contains(username)) {
                        logger.debug(username + " is allowed to access a resource.");
                        return true;
                    }
                    else {
                        logger.debug(username + " is NOT allowed to access a resource!");
                        return false;
                    }
                }
                else {
                    // Always return true
                    return true;
                }
            }
            else {
                throw new Exception("Invalid certificate distinguished name.");
            }
        }
        catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED).
                            entity(e.getMessage()).
                            build()
            );
        }
    }
}

package eu.contrail.security;

import org.ow2.contrail.common.oauth.client.TokenInfo;
import org.ow2.contrail.common.oauth.client.TokenValidator;
import org.ow2.contrail.federation.federationdb.jpa.dao.UserDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

public class OAuthFilter implements Filter {
    private ServletContext ctx;

    private TokenValidator tokenValidator;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.ctx = filterConfig.getServletContext();

        try {
            URI checkTokenEndpointUri = new URI(filterConfig.getInitParameter("checkTokenEndpointUri"));
            String keystoreFile = filterConfig.getInitParameter("keystoreFile");
            String keystorePass = filterConfig.getInitParameter("keystorePass");
            String truststoreFile = filterConfig.getInitParameter("truststoreFile");
            String truststorePass = filterConfig.getInitParameter("truststorePass");

            tokenValidator = new TokenValidator(checkTokenEndpointUri, keystoreFile, keystorePass,
                    truststoreFile, truststorePass);
        }
        catch (URISyntaxException e) {
            throw new ServletException("Failed to initialize OAuthFilter: " + e.getMessage());
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        try {
            TokenInfo tokenInfo = tokenValidator.checkToken(httpRequest);

            User user = new UserDAO().findByUuid(tokenInfo.getOwnerUuid());
            if (user == null) {
                throw new TokenValidator.InvalidOAuthTokenException(
                        "The user specified in the access token cannot be found.");
            }
            httpRequest.setAttribute("user", user);
            httpRequest.setAttribute("access_token", tokenInfo.getAccessToken());
        }
        catch (TokenValidator.InvalidOAuthTokenException e) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        }
        catch (TokenValidator.InvalidCertificateException e) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        }
        catch (Exception e) {
            ctx.log("Failed to validate OAuth access token: " + e.getMessage(), e);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stacktrace = sw.toString();
            ctx.log("Stacktrace:" + stacktrace);
            throw new ServletException("Failed to validate OAuth access token.");
        }


        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}

package org.ow2.contrail.common.oauth.client;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

public class OAuthFilter implements Filter {
    private static Logger log = Logger.getLogger(OAuthFilter.class);

    private static final String TOKEN_INFO_ATTR = "access_token_info";

    private TokenValidator tokenValidator;
    private Boolean testMode;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.debug("Initializing OAuthFilter...");

        String configFilePath = filterConfig.getInitParameter("configuration-file");
        if (configFilePath == null) {
            throw new ServletException("OAuthFilter: missing parameter 'configuration-file'.");
        }
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configFilePath));
            log.debug(String.format("Properties loaded successfully from file '%s'.", configFilePath));
        }
        catch (IOException e) {
            String message = String.format("Failed to read properties file '%s': %s", configFilePath, e.getMessage());
            log.error(message, e);
            throw new ServletException(message, e);
        }

        try {
            // test mode init param
            testMode = Boolean.valueOf(filterConfig.getInitParameter("test-mode"));
            if (testMode) {
                log.warn("OAuthFilter is running in test mode. Non-SSL requests are allowed without authentication.");
            }

            String validationEndpoint = props.getProperty("oauthFilter.validationEndpoint");
            String keystoreFile = props.getProperty("oauthFilter.keystore.file");
            String keystorePass = props.getProperty("oauthFilter.keystore.pass");
            String truststoreFile = props.getProperty("oauthFilter.truststore.file");
            String truststorePass = props.getProperty("oauthFilter.truststore.pass");

            tokenValidator = new TokenValidator(new URI(validationEndpoint), keystoreFile, keystorePass,
                    truststoreFile, truststorePass);

            log.info("OAuthFilter initialized successfully.");
        }
        catch (Exception e) {
            log.error("Failed to initialize OAuthFilter: " + e.getMessage(), e);
            throw new ServletException("Failed to initialize OAuthFilter: " + e.getMessage(), e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        if (!httpRequest.isSecure()) {
            if (testMode) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
            else {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection is required.");
                return;
            }
        }

        try {
            TokenInfo tokenInfo = tokenValidator.checkToken(httpRequest);

            // store tokenInfo to httpRequest object
            httpRequest.setAttribute(TOKEN_INFO_ATTR, tokenInfo);
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
            log.error("Failed to validate OAuth access token: " + e.getMessage(), e);
            throw new ServletException("Failed to validate OAuth access token.");
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    public static TokenInfo getAccessTokenInfo(HttpServletRequest httpRequest) {
        return (TokenInfo) httpRequest.getAttribute(TOKEN_INFO_ATTR);
    }

    @Override
    public void destroy() {
    }
}

package org.consec.oauth2.authzserver.utils;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;

public class ClientCertAuthFilter implements Filter {
    private static Logger log = Logger.getLogger(ClientCertAuthFilter.class);
    private Boolean testMode;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.trace("Initializing ClientCertAuthFilter.");

        try {
            // test mode init param
            testMode = Boolean.valueOf(filterConfig.getInitParameter("test-mode"));
            if (testMode) {
                log.warn("ClientCertAuthFilter is running in test mode. Non-SSL requests are allowed without authentication.");
            }
        }
        catch (Exception e) {
            throw new ServletException("Failed to initialize ClientCertAuthFilter: " + e.getMessage());
        }

        log.trace("ClientCertAuthFilter initialized successfully.");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        if (testMode && !httpRequest.isSecure()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        try {
            X509Certificate[] certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
            if (certs == null || certs.length == 0) {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        catch (Exception e) {
            log.error("Error encountered while authorizing the request: " + e.getMessage(), e);
            throw new ServletException("Error encountered while authorizing the request. " +
                    "Please see log file for details.");
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}

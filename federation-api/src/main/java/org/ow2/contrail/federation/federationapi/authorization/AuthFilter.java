package org.ow2.contrail.federation.federationapi.authorization;

import org.apache.log4j.Logger;
import org.ow2.contrail.federation.federationapi.exceptions.AuthorizationException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthFilter implements Filter {
    private static Logger log = Logger.getLogger(AuthFilter.class);
    private Boolean testMode;
    private Authorizer authorizer;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.trace("Initializing AuthFilter.");

        try {
            ServletContext servletContext = filterConfig.getServletContext();
            WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
            AutowireCapableBeanFactory autowireCapableBeanFactory = webApplicationContext.getAutowireCapableBeanFactory();
            authorizer = (Authorizer) autowireCapableBeanFactory.getBean("authorizer");

            // test mode init param
            testMode = Boolean.valueOf(filterConfig.getInitParameter("test-mode"));
            if (testMode) {
                log.warn("AuthFilter is running in test mode. Non-SSL requests are allowed without authentication.");
            }
        }
        catch (Exception e) {
            throw new ServletException("Failed to initialize AuthFilter: " + e.getMessage());
        }

        log.trace("AuthFilter initialized successfully.");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        if (testMode && !httpRequest.isSecure()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        try {
            boolean isAuthorized = authorizer.isAuthorized(httpRequest);

            if (!isAuthorized) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        catch (AuthorizationException e) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Failed to authorize the request: " + e.getMessage());
            return;
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

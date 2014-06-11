package org.consec.authz.xacml.filter;

import org.apache.log4j.Logger;
import org.consec.authz.xacml.common.XACMLDecision;
import org.consec.authz.xacml.filter.utils.PDPClient;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

public class XACMLAuthzFilter implements Filter {
    private static Logger log = Logger.getLogger(XACMLAuthzFilter.class);
    private PDPClient pdpClient;
    private boolean testMode;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.trace("Initializing XACMLAuthzFilter...");

        try {
            String configFilePath = filterConfig.getInitParameter("configuration-file");
            if (configFilePath == null) {
                throw new ServletException("XACMLAuthzFilter: missing init parameter 'configuration-file' in web.xml.");
            }
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(configFilePath));
                log.debug(String.format("Properties loaded successfully from file '%s'.", configFilePath));
            }
            catch (IOException e) {
                throw new Exception(String.format("Failed to read properties file '%s': %s",
                        configFilePath, e.getMessage()));
            }

            String pdpEndpoint = props.getProperty("xacml-authz-filter.pdpEndpoint");
            String keystoreFile = props.getProperty("xacml-authz-filter.keystore.file");
            String keystorePass = props.getProperty("xacml-authz-filter.keystore.pass");
            String truststoreFile = props.getProperty("xacml-authz-filter.truststore.file");
            String truststorePass = props.getProperty("xacml-authz-filter.truststore.pass");

            // TODO: is PDPClient thread-safe?
            pdpClient = new PDPClient(new URI(pdpEndpoint), keystoreFile, keystorePass, truststoreFile, truststorePass);

            // test mode init param
            testMode = Boolean.valueOf(filterConfig.getInitParameter("test-mode"));
            if (testMode) {
                log.warn("XACMLAuthzFilter is running in test mode. Non-SSL requests are allowed without " +
                        "authorization.");
            }

            log.info("XACMLAuthzFilter initialized successfully.");
        }
        catch (Exception e) {
            log.error("Failed to initialize XACMLAuthzFilter: " + e.getMessage(), e);
            throw new ServletException("Failed to initialize XACMLAuthzFilter: " + e.getMessage(), e);
        }

        log.trace("XACMLAuthzFilter initialized successfully.");
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

        XACMLDecision xacmlDecision;
        try {
            xacmlDecision = pdpClient.evaluate(httpRequest);
        }
        catch (Exception e) {
            log.error("Failed to authorize the request: " + e.getMessage(), e);
            throw new ServletException("Failed to authorize the request. " +
                    "Please contact the administrator.");
        }

        if (xacmlDecision == XACMLDecision.PERMIT) {
            filterChain.doFilter(servletRequest, servletResponse);
        }
        else {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Override
    public void destroy() {
    }
}

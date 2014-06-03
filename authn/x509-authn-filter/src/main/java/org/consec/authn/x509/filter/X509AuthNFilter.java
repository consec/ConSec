package org.consec.authn.x509.filter;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.authn.x509.filter.exceptions.AuthNException;
import org.consec.authn.x509.filter.utils.RESTClient;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class X509AuthNFilter implements Filter {
    private static Logger log = Logger.getLogger(X509AuthNFilter.class);
    private RESTClient restClient;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.trace("Initializing X509AuthNFilter...");

        try {
            String configFilePath = filterConfig.getInitParameter("configuration-file");
            if (configFilePath == null) {
                throw new ServletException("X509AuthNFilter: missing init parameter 'configuration-file' in web.xml.");
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

            String endpoint = props.getProperty("x509-authn-filter.federation-api.endpoint");
            String keystoreFile = props.getProperty("x509-authn-filter.keystore.file");
            String keystorePass = props.getProperty("x509-authn-filter.keystore.pass");
            String truststoreFile = props.getProperty("x509-authn-filter.truststore.file");
            String truststorePass = props.getProperty("x509-authn-filter.truststore.pass");

            restClient = new RESTClient(endpoint, keystoreFile, keystorePass, truststoreFile, truststorePass);

            log.info("X509AuthNFilter initialized successfully.");
        }
        catch (Exception e) {
            log.error("Failed to initialize X509AuthNFilter: " + e.getMessage(), e);
            throw new ServletException("Failed to initialize X509AuthNFilter: " + e.getMessage(), e);
        }

        log.trace("X509AuthNFilter initialized successfully.");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        try {
            X509Certificate[] certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
            if (certs == null) {
                throw new AuthNException("The client certificate is missing.");
            }

            String principalId = null;
            try {
                Principal principal = certs[0].getSubjectDN();
                String dn = principal.getName();
                Pattern dnPattern = Pattern.compile("CN=([\\w-]+)");
                Matcher m = dnPattern.matcher(dn);
                if (m.find()) {
                    principalId = m.group(1);
                }
                else {
                    throw new AuthNException("Invalid client certificate distinguished name.");
                }
            }
            catch (Exception e) {
                throw new AuthNException("Invalid client certificate.");
            }

            // retrieve principal from the federation-api
            String principalData = restClient.get("/users/" + principalId);
            if (principalData == null) {
                throw new AuthNException("Invalid principal.");
            }
            JSONObject principalJson = new JSONObject(principalData);
            String userId = principalJson.getString("userId");
            JSONArray userRoles = principalJson.getJSONArray("roles");
            JSONArray userGroups = principalJson.getJSONArray("groups");

            httpRequest.setAttribute("CONSEC_USER_ID", userId);
            httpRequest.setAttribute("CONSEC_USER_ROLES", convertToList(userRoles));
            httpRequest.setAttribute("CONSEC_USER_GROUPS", convertToList(userGroups));
        }
        catch (AuthNException e) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication failed: " + e.getMessage());
            return;
        }
        catch (Exception e) {
            log.error("Failed to authenticate the request: " + e.getMessage(), e);
            throw new ServletException("Error encountered while authenticating the request. " +
                    "Please see log file for details.");
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }

    private List<String> convertToList(JSONArray arr) throws JSONException {
        List<String> list = new ArrayList<String>();
        for (int i=0; i<arr.length(); i++) {
            list.add(arr.getString(i));
        }
        return list;
    }
}

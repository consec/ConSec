package org.ow2.contrail.federation.federationapi;

import org.apache.log4j.Logger;
import org.consec.federationdb.utils.EMF;
import org.ow2.contrail.common.oauth.client.atmanager.MemoryOAuthATManager;
import org.ow2.contrail.common.oauth.client.atmanager.OAuthATManagerFactory;
import org.ow2.contrail.federation.federationapi.utils.Conf;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

public class MyServletContextListener implements ServletContextListener {

    protected static Logger logger =
            Logger.getLogger(MyServletContextListener.class);

    /**
     * These are servlet properties from configuration file
     */
    protected static Properties servletProperties = new Properties();

    /**
     * List of CNs which are authorized to access the API.
     */
    protected static ArrayList<String> authzList = new ArrayList<String>();

    /**
     * Is authorization check enabled?
     */
    protected static boolean authZEnabled = false;

    /**
     * Is authorization check enabled?
     */
    protected static boolean fedIdProvEnabled = false;

    /**
     * Which federation core are we using.
     */
    protected static String federCoreClass = null;

    private ServletContext context = null;

    /**
     * Holds an indentity provider URI.
     */
    protected static String federationIdentityProviderURI = null;

    /**
     * This method is invoked when the Web Application is ready to service requests
     */
    public void contextInitialized(ServletContextEvent event) {
        logger.debug("Entering contextInitialized()");
        this.context = event.getServletContext();

        try {
            String configFilePath = context.getInitParameter("properties-file");
            if (configFilePath == null) {
                throw new Exception("Missing parameter 'properties-file' in web.xml file.");
            }

            File confFile = new File(configFilePath);
            if (!confFile.canRead()) {
                throw new Exception("Failed to open configuration file " + confFile);
            }

            // load configuration file
            Conf.getInstance().load(confFile);

            EMF.init("mainPU");

            // initialize OAuth access token manager
            MemoryOAuthATManager oAuthATManager = new MemoryOAuthATManager(
                    context,
                    Conf.getInstance().getAddressOAuthAS(),
                    Conf.getInstance().getOAuthClientKeystoreFile(),
                    Conf.getInstance().getOAuthClientKeystorePass(),
                    Conf.getInstance().getOAuthClientTruststoreFile(),
                    Conf.getInstance().getOAuthClientTruststorePass(),
                    Conf.getInstance().getOAuthClientId(),
                    Conf.getInstance().getOAuthClientSecret()
                    );
            OAuthATManagerFactory.setOAuthATManager(oAuthATManager);
        }
        catch (Exception e) {
            logger.error("Federation-api failed to start: " + e.getMessage());
            throw new RuntimeException(e);
        }

        logger.debug("contextInitialized() finished successfully.");
        logger.info("Federation-api was initialized successfully.");
    }

    /**
     * This method is invoked when the Web Application has been removed
     * and is no longer able to accept requests
     */

    public void contextDestroyed(ServletContextEvent event) {
        System.out.println("Federation-api webapp has been removed.");
        this.context = null;

    }

    public static ArrayList<String> getAuthzList() {
        return authzList;
    }

    public static void setAuthzList(ArrayList<String> authzList) {
        MyServletContextListener.authzList = authzList;
    }

    /**
     * Gets server conf
     * @param key
     * @return
     * @throws Exception
     */
    public static String getConfProperty(String key) throws Exception {
        String value = servletProperties.getProperty(key);
        if (value == null || value.equals("")) {
            throw new Exception(String.format("Invalid configuration file: missing property '%s'.", key));
        }
        return value;
    }

}
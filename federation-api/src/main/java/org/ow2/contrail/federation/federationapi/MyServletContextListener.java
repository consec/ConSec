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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class MyServletContextListener implements ServletContextListener {

    protected static Logger logger =
            Logger.getLogger(MyServletContextListener.class);

    /**
     * These are servlet properties from configuration file
     */
    protected static Properties servletProperties = new Properties();

    public static String INIT_PARAM_AUTHZ_ENABLED = "authz-enabled";
    public static String INIT_PARAM_AUTHZ_ENGINE = "authz-engine";
    public static String INIT_PARAM_AUTHZ_XACML_PDP_URL = "SoapXACMLAuthorizer-pdp-url";
    public static String INIT_PARAM_FEDER_CORE_CLASS = "federation-core-class";
    public static String INIT_PARAM_FEDERATION_ID_PROV_ENABLED = "Federation-id-prov.enabled";
    public static String INIT_PARAM_FEDERATION_ID_PROV_URI = "Federation-id-prov.uri";

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
     * Reads configuration file and tries to setup the values.
     *
     * @param propertyFile
     * @throws Exception
     */
    protected static void setup(File propertyFile) throws Exception {
        logger.debug("Entering setup");
        try {
            FileInputStream fis = new FileInputStream(propertyFile);
            servletProperties.load(fis);
            fis.close();
            logger.debug(servletProperties);
            try {
                authZEnabled = Boolean.parseBoolean(servletProperties.getProperty(INIT_PARAM_AUTHZ_ENABLED));
                logger.debug("AuthZenabled:" + authZEnabled);
                federCoreClass = servletProperties.getProperty(INIT_PARAM_FEDER_CORE_CLASS);
                logger.debug("federCoreClass : " + federCoreClass);
                fedIdProvEnabled = Boolean.parseBoolean(servletProperties.getProperty(INIT_PARAM_FEDERATION_ID_PROV_ENABLED));
                logger.debug("fedIdProvEnabled:" + fedIdProvEnabled);
                federationIdentityProviderURI = servletProperties.getProperty(INIT_PARAM_FEDERATION_ID_PROV_URI);
                logger.debug("federationIdentityProviderURI:" + federationIdentityProviderURI);
            }
            catch (Exception err) {
                logger.error(err.getMessage());
            }
        }
        catch (FileNotFoundException e) {
            logger.error("Property file not found.");
            logger.error(e.getMessage());
            throw e;
        }
        catch (IOException e) {
            logger.error(e.getMessage());
            throw e;
        }
        logger.debug("Exiting setup");
    }

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

            logger.debug("Reading configuration file " + configFilePath);
            try {
                setup(confFile);
                logger.debug("Conf file was read successfully.");
            }
            catch (Exception e) {
                throw new Exception(String.format("Failed to load configuration file %s: %s",
                        configFilePath, e.getMessage()));
            }

            // load configuration file
            Conf.getInstance().load(confFile);

            String appDataRoot = context.getInitParameter("app-data-root");
            if (appDataRoot == null) {
                throw new Exception("Missing parameter 'app-data-root' in web.xml file.");
            }
            Conf.getInstance().setAppDataRoot(appDataRoot);

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
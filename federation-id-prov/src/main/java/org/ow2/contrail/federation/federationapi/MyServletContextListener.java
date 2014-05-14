package org.ow2.contrail.federation.federationapi;

import org.apache.log4j.Logger;
import org.opensaml.saml2.core.Issuer;
import org.ow2.contrail.federation.federationapi.saml.SAML;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

public class MyServletContextListener implements ServletContextListener {

    protected static Logger logger =
            Logger.getLogger(MyServletContextListener.class);

    /**
     * These are servlet properties from file {@link #propertiesFilePath}.
     */
    protected static Properties servletProperties = new Properties();

    /**
     * Path to properties file.
     */
    protected static String propertiesFilePath = "/etc/contrail/federation-id-prov/federation-id-prov.conf";

    /**
     * List of CNs which are authorized to access the API.
     */
    protected static ArrayList<String> authzList = new ArrayList<String>();

    /**
     * Is authorization check enabled?
     */
    protected static boolean authZEnabled = false;

    private ServletContext context = null;

    /**
     * This is used in SAML Assertions.
     */
    private static Issuer issuer = SAML.create(Issuer.class, Issuer.DEFAULT_ELEMENT_NAME);

    /**
     * From the property file.
     */
    private static String serverName;

    /**
     * Reads {@link #propertiesFilePath} and tries to setup the values.
     *
     * @param propertyFile
     * @throws Exception
     */
    protected void setup(File propertyFile) throws Exception {
        logger.debug("Entering setup");
        String authZfile = null;
        try {
            FileInputStream fis = new FileInputStream(propertyFile);
            this.servletProperties.load(fis);
            fis.close();
            logger.debug(this.servletProperties);
            try {
                authZEnabled = Boolean.parseBoolean(this.servletProperties.getProperty("authz-enabled"));
                logger.debug("AuthZenabled:" + authZEnabled);
                authZfile = this.servletProperties.getProperty("authz-file");
                logger.debug("Obtained authZfile:" + authZfile);
                serverName = this.servletProperties.getProperty("serverName");
                logger.debug("Obtained serverName:" + serverName);
            }
            catch (Exception err) {
                logger.error(err.getMessage());
            }
            try {
                if (authZEnabled) readAuthZFile(new File(authZfile));
            }
            catch (Exception e) {
                logger.error(e.getMessage());
                authZEnabled = false;
                e.printStackTrace();
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
        logger.debug("Entering contextInitialized");
        this.context = event.getServletContext();
        PersistenceUtils.createInstance("appPU");
        String configFilePath = context.getInitParameter("properties-file");
        String authZfile = null;
        if (configFilePath != null) {
            logger.debug("Obtained property file:" + configFilePath);
            this.propertiesFilePath = configFilePath;
            File confFile = new File(this.propertiesFilePath);
            if (confFile.exists()) {
                logger.debug("Reading property file:" + this.propertiesFilePath);
                try {
                    setup(confFile);
                }
                catch (Exception e) {
                    logger.error(e.getMessage());
                    logger.error(FederationDBCommon.getStackTrace(e));
                }
            }
            else {
                logger.debug("Property file " + this.propertiesFilePath + " does not exist. Assuming default values.");
                this.authZEnabled = false;
                this.authzList = new ArrayList<String>();
                serverName = "http://localhost:8080/federation-id-prov/saml";
                logger.debug("Simple authorization is disabled.");
            }
        }
        logger.debug("Setting issuer");
        issuer.setValue(serverName);
        logger.debug("Federation-id-prov webapp has started.");
    }

    /**
     * Read the list of CNs which are authorized to use the API.
     *
     * @param file
     */
    private void readAuthZFile(File file) throws Exception {
        logger.debug("Entering readAuthZFile");
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            authzList.add(line);
        }
        bufferedReader.close();
        logger.debug("Authorized CNs: " + authzList);
        logger.debug("Exiting readAuthZFile");
    }

    /**
     * This method is invoked when the Web Application has been removed
     * and is no longer able to accept requests
     */

    public void contextDestroyed(ServletContextEvent event) {
        System.out.println("Federation-id-prov webapp has been removed.");
        this.context = null;

    }

    public static ArrayList<String> getAuthzList() {
        return authzList;
    }

    public static void setAuthzList(ArrayList<String> authzList) {
        MyServletContextListener.authzList = authzList;
    }

    public static boolean isAuthZEnabled() {
        return authZEnabled;
    }

    public static String getServerName() {
        return serverName;
    }

    public static void setServerName(String serverName) {
        MyServletContextListener.serverName = serverName;
    }

    public static Issuer getIssuer() {
        return issuer;
    }

    public static void setIssuer(Issuer issuer) {
        MyServletContextListener.issuer = issuer;
    }

}
package org.consec.dynamicca.utils;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.consec.dynamicca.jpa.EMF;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.security.Security;

public class WebAppInitializer implements ServletContextListener {
    protected static Logger log = Logger.getLogger(WebAppInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            ServletContext context = servletContextEvent.getServletContext();
            String configFilePath = context.getInitParameter("conf-file");
            if (configFilePath == null) {
                throw new RuntimeException("Missing parameter 'conf-file' in web.xml file.");
            }

            // load configuration file
            Conf.getInstance().load(new File(configFilePath));

            // initialize JPA context
            EMF.init();

            // register BouncyCastleProvider
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            log.info("Dynamic-ca-server was initialized successfully.");
        }
        catch (Exception e) {
            log.error("Failed to initialize dynamic-ca-server: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize dynamic-ca-server.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}

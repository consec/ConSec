package org.ow2.contrail.federation.auditmanager.utils;

import org.apache.log4j.Logger;
import org.ow2.contrail.common.oauth.client.atmanager.MemoryOAuthATManager;
import org.ow2.contrail.common.oauth.client.atmanager.OAuthATManager;
import org.ow2.contrail.common.oauth.client.atmanager.OAuthATManagerFactory;
import org.ow2.contrail.federation.auditmanager.scheduler.SchedulerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class WebAppInitializer implements ServletContextListener {
    protected static Logger log = Logger.getLogger(WebAppInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        log.debug("Initializing audit-manager...");
        try {
            ServletContext context = servletContextEvent.getServletContext();
            String configFilePath = context.getInitParameter("configuration-file");
            if (configFilePath == null) {
                throw new RuntimeException("Missing parameter 'configuration-file' in web.xml file.");
            }

            // load configuration file
            Conf.getInstance().load(configFilePath);

            // initialize MongoDB connection
            MongoDBConnection.init();

            // initialize scheduler
            SchedulerFactory.initScheduler();

            // initialize OAuthATManager
            OAuthATManager oauthATManager = new MemoryOAuthATManager(
                    Conf.getInstance().getAddressOAuthAS(),
                    Conf.getInstance().getKeystoreFile(), Conf.getInstance().getKeystorePass(),
                    Conf.getInstance().getTruststoreFile(), Conf.getInstance().getTruststorePass(),
                    Conf.getInstance().getOAuthClientId(), Conf.getInstance().getOAuthClientSecret());
            OAuthATManagerFactory.setOAuthATManager(oauthATManager);

            log.info("Audit-manager was initialized successfully.");
        }
        catch (Exception e) {
            log.error("Failed to initialize audit-manager: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize audit-manager.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        MongoDBConnection.close();
    }
}

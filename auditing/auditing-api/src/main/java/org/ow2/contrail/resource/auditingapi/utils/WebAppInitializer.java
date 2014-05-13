package org.ow2.contrail.resource.auditingapi.utils;

import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class WebAppInitializer implements ServletContextListener {
    protected static Logger log = Logger.getLogger(WebAppInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        log.debug("Initializing auditing-api...");
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

            log.info("Auditing-api was initialized successfully.");
        }
        catch (Exception e) {
            log.error("Failed to initialize auditing-api: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize auditing-api.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        MongoDBConnection.close();
    }
}

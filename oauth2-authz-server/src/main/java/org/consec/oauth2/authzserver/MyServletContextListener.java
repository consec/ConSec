package org.consec.oauth2.authzserver;

import org.apache.log4j.Logger;
import org.consec.oauth2.authzserver.utils.Configuration;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

public class MyServletContextListener implements ServletContextListener {
    protected static Logger log = Logger.getLogger(MyServletContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        log.trace("contextInitialized() started.");

        try {
            ServletContext context = servletContextEvent.getServletContext();
            PersistenceUtils.createInstance("AuthzServerPersistenceUnit");

            String configFilePath = context.getInitParameter("conf-file");
            if (configFilePath == null) {
                throw new RuntimeException("Missing parameter 'conf-file' in web.xml file.");
            }

            // load configuration file
            File confFile = new File(configFilePath);
            Configuration.getInstance().load(confFile);
        }
        catch (Exception e) {
            log.error("contrail-oauth-as failed to start: " + e.getMessage());
            throw new RuntimeException(e);
        }

        log.info("contrail-oauth-as was initialized successfully.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}

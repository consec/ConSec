package org.ow2.contrail.common.oauth.demo;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ow2.contrail.common.oauth.demo.utils.Conf;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.security.Security;

public class MyServletContextListener implements ServletContextListener {
    private static Logger log = Logger.getLogger(Conf.class);

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {

        try {
            Security.addProvider(new BouncyCastleProvider());

            ServletContext context = contextEvent.getServletContext();
            String configFilePath = context.getInitParameter("configuration-file");
            if (configFilePath == null) {
                throw new Exception("Missing setting 'configuration-file' in web.xml.");
            }

            Conf.getInstance().load(new File(configFilePath));
        }
        catch (Exception e) {
            log.error("OAuth-java-client-demo webapp failed to start: " + e.getMessage());
            throw new RuntimeException("OAuth-java-client-demo webapp failed to start: " + e.getMessage());
        }

        log.info("OAuth-java-client-demo was initialized successfully.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent contextEvent) {
    }
}
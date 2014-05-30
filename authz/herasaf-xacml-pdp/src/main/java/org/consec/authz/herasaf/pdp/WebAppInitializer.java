package org.consec.authz.herasaf.pdp;

import org.apache.log4j.Logger;
import org.consec.authz.herasaf.pdp.core.FileBasedPolicyRepository;
import org.consec.authz.herasaf.pdp.core.HerasafXACMLEngine;
import org.consec.authz.herasaf.pdp.utils.Conf;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

public class WebAppInitializer implements ServletContextListener {
    protected static Logger log = Logger.getLogger(WebAppInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            ServletContext context = servletContextEvent.getServletContext();
            String configFilePath = context.getInitParameter("conf-file");
            if (configFilePath == null) {
                throw new Exception("Missing parameter 'conf-file' in web.xml file.");
            }

            // load configuration file
            Conf.load(new File(configFilePath));

            // initialize Heras-af XACML engine
            File repositoryLocation = new File(Conf.getPolicyRepositoryLocation());
            FileBasedPolicyRepository repository = new FileBasedPolicyRepository(repositoryLocation);
            HerasafXACMLEngine herasafXACMLEngine = new HerasafXACMLEngine(repository);
            repository.restoreRepository();
            context.setAttribute("herasafXACMLEngine", herasafXACMLEngine);

            log.info("Herasaf-xacml-pdp was initialized successfully.");
        }
        catch (Exception e) {
            log.error("Failed to initialize Herasaf-xacml-pdp: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Herasaf-xacml-pdp.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}

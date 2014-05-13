package eu.contrail.security;

import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class MyServletContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        PersistenceUtils.createInstance("appPU");
    }

    @Override
    public void contextDestroyed(ServletContextEvent contextEvent) {
    }
}
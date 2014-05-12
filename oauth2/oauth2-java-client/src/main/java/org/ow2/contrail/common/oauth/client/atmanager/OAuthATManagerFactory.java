package org.ow2.contrail.common.oauth.client.atmanager;

import org.apache.log4j.Logger;

public class OAuthATManagerFactory {
    private static Logger log = Logger.getLogger(OAuthATManagerFactory.class);
    private static OAuthATManager oAuthATManager;

    public static void setOAuthATManager(OAuthATManager oAuthATManager) {
        OAuthATManagerFactory.oAuthATManager = oAuthATManager;
    }

    public static OAuthATManager getOAuthATManager() {
        return oAuthATManager;
    }
}

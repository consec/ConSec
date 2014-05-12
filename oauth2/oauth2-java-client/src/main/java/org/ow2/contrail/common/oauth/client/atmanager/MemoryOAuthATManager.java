package org.ow2.contrail.common.oauth.client.atmanager;

import org.apache.log4j.Logger;
import org.ow2.contrail.common.oauth.client.AccessToken;
import org.ow2.contrail.common.oauth.client.CCFlowClient;
import org.ow2.contrail.common.oauth.client.utils.Conf;

import javax.servlet.ServletContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MemoryOAuthATManager implements OAuthATManager {
    private static Logger log = Logger.getLogger(MemoryOAuthATManager.class);

    private Map<String, AccessToken> accessTokenCache;
    private CCFlowClient ccFlowClient;

    public MemoryOAuthATManager(URI oauthASAddress,
                                String keystoreFile, String keystorePass,
                                String truststoreFile, String truststorePass,
                                String clientId, String clientSecret) throws URISyntaxException {
        accessTokenCache = new HashMap<String, AccessToken>();
        init(oauthASAddress, keystoreFile, keystorePass, truststoreFile, truststorePass, clientId, clientSecret);

        log.debug("MemoryOAuthATManager initialized successfully.");
    }

    public MemoryOAuthATManager(ServletContext context,
                                URI oauthASAddress,
                                String keystoreFile, String keystorePass,
                                String truststoreFile, String truststorePass,
                                String clientId, String clientSecret) throws URISyntaxException {
        accessTokenCache = new HashMap<String, AccessToken>();
        context.setAttribute("oauthAccessTokenCache", accessTokenCache);
        init(oauthASAddress, keystoreFile, keystorePass, truststoreFile, truststorePass, clientId, clientSecret);

        log.debug("MemoryOAuthATManager initialized successfully.");
    }

    private void init(URI oauthASAddress,
                      String keystoreFile, String keystorePass,
                      String truststoreFile, String truststorePass,
                      String clientId, String clientSecret) throws URISyntaxException {

        URI tokenEndpoint = oauthASAddress.resolve(Conf.OAUTH_AS_TOKEN_ENDPOINT);
        ccFlowClient = new CCFlowClient(
                tokenEndpoint,
                keystoreFile, keystorePass,
                truststoreFile, truststorePass);
        ccFlowClient.setClientId(clientId);
        ccFlowClient.setClientSecret(clientSecret);
    }

    @Override
    public AccessToken getAccessToken(String resourceOwnerUuid) throws Exception {
        log.debug("Retrieving access token for the resource owner " + resourceOwnerUuid);
        if (accessTokenCache.containsKey(resourceOwnerUuid)) {
            log.debug("Access token found in the cache.");
            AccessToken accessToken = accessTokenCache.get(resourceOwnerUuid);
            Date now = new Date();
            if (accessToken.getExpireTime().getTime() - now.getTime() > 60 * 1000) {
                log.debug("Returning access token " + accessToken.getValue());
                return accessToken;
            }
            else {
                log.debug("Access token has expired.");
                accessTokenCache.remove(resourceOwnerUuid);
            }
        }

        log.debug("Requesting new access token from the Authorization server.");
        AccessToken accessToken = null;
        try {
            accessToken = ccFlowClient.requestAccessToken(resourceOwnerUuid, "");
            log.debug("Access token obtained successfully.");
        }
        catch (Exception e) {
            log.debug("Failed to obtain an access token: " + e.getMessage(), e);
            throw new Exception(String.format("Failed to obtain an OAuth access token: " + e.getMessage()));
        }

        accessTokenCache.put(resourceOwnerUuid, accessToken);
        log.debug("Returning access token " + accessToken.getValue());

        return accessToken;
    }
}

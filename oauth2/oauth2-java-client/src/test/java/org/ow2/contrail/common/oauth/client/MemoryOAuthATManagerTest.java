package org.ow2.contrail.common.oauth.client;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ow2.contrail.common.oauth.client.atmanager.MemoryOAuthATManager;
import org.ow2.contrail.common.oauth.client.atmanager.OAuthATManager;
import org.ow2.contrail.common.oauth.client.atmanager.OAuthATManagerFactory;

import java.io.FileInputStream;
import java.net.URI;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore
public class MemoryOAuthATManagerTest {
    private Properties props;

    @Before
    public void initialize() throws Exception {
        props = new Properties();
        props.load(new FileInputStream("src/test/resources/oauth-java-client.properties"));
        MemoryOAuthATManager memoryOAuthATManager = new MemoryOAuthATManager(
                new URI(props.getProperty("oauth-as.address")),
                props.getProperty("keystore.file"), props.getProperty("keystore.pass"),
                props.getProperty("truststore.file"), props.getProperty("truststore.pass"),
                props.getProperty("client.id"), props.getProperty("client.secret")
        );
        OAuthATManagerFactory.setOAuthATManager(memoryOAuthATManager);
    }

    @Test
    public void testObtainAccessToken() throws Exception {
        OAuthATManager oAuthATManager = OAuthATManagerFactory.getOAuthATManager();

        String resourceOwnerUuid = props.getProperty("resourceOwner.uuid");
        AccessToken accessToken = oAuthATManager.getAccessToken(resourceOwnerUuid);
        assertNotNull(accessToken);

        // access token should now be retrieved from the cache
        AccessToken accessToken1 = oAuthATManager.getAccessToken(resourceOwnerUuid);
        assertEquals(accessToken.getValue(), accessToken1.getValue());
    }
}

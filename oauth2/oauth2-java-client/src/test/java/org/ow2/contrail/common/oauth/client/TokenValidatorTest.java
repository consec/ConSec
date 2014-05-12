package org.ow2.contrail.common.oauth.client;

import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;

@Ignore
public class TokenValidatorTest {

    @Test
    public void checkTokenTest() throws Exception {
        URI endpointUri = new URI("https://localhost:9443/oauth/r/access_token/check");
        String accessToken = "b1a69983-fdd4-3c2f-a4e0-64f9f68391e0";
        String keystoreFile = "src/test/resources/client.jks";
        String keystorePass = "contrail";
        String truststoreFile = "src/test/resources/cacerts.jks";
        String truststorePass = "contrail";

        TokenValidator tokenValidator = new TokenValidator(endpointUri, keystoreFile, keystorePass,
                truststoreFile, truststorePass);

        TokenInfo tokenInfo = tokenValidator.checkToken(accessToken, "TestClient");

        System.out.println(tokenInfo.toString());
    }
}

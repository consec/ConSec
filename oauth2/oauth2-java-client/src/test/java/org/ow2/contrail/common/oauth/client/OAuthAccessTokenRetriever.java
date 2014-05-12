package org.ow2.contrail.common.oauth.client;

import org.apache.amber.oauth2.client.OAuthClient;
import org.apache.amber.oauth2.client.URLConnectionClient;
import org.apache.amber.oauth2.client.request.OAuthClientRequest;
import org.apache.amber.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.amber.oauth2.common.message.types.ResponseType;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class OAuthAccessTokenRetriever {

    public static void main(String[] args) throws Exception {
        String clientId = "22";

        // truststore with oauth server certificate
        System.setProperty("javax.net.ssl.trustStore", "src/test/resources/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "contrail");

        try {
            OAuthClientRequest request = OAuthClientRequest
                    .authorizationLocation("https://localhost:5000/oauth/authorize")
                    .setClientId(clientId)
                    .setRedirectURI("http://localhost:5002/oauth2/oauth_redirect")
                    .setResponseType(ResponseType.CODE.toString())
                    .setScope("https://localhost:5000/oauth/certificate/")
                    .buildQueryMessage();

            //in web application you make redirection to uri:
            System.out.println("Visit: " + request.getLocationUri() + "\nand grant permission");

            System.out.print("Now enter the OAuth code you have received in redirect uri: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String code = br.readLine();

            request = OAuthClientRequest
                    .tokenLocation("https://localhost:5000/oauth/access_token")
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(clientId)
                    .setRedirectURI("http://localhost:5002/oauth2/oauth_redirect")
                    .setCode(code)
                    .setScope("https://localhost:5000/oauth/certificate/")
                    .buildBodyMessage();

            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);

            System.out.println(
                    "Access Token: " + oAuthResponse.getAccessToken() + ", Expires in: " + oAuthResponse
                            .getExpiresIn());
        }
        catch (OAuthProblemException e) {
            System.out.println("OAuth error: " + e.getError());
            System.out.println("OAuth error description: " + e.getDescription());
        }
    }
}

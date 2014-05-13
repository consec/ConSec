package org.ow2.contrail.common.oauth.demo.utils;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.ow2.contrail.common.oauth.client.TokenInfo;
import org.ow2.contrail.common.oauth.client.TokenValidator;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class TokenUtils {
    public static String getAuthorizationRequestUri(String state) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("response_type", "code"));
        params.add(new BasicNameValuePair("redirect_uri", Conf.getInstance().getClientOauth2CallbackUri()));
        params.add(new BasicNameValuePair("client_id", Conf.getInstance().getClientId()));
        params.add(new BasicNameValuePair("state", state));
        params.add(new BasicNameValuePair("scope", Conf.getInstance().getScope()));

        String query = URLEncodedUtils.format(params, "utf-8");
        return Conf.getInstance().getASAuthorizationUri() + "?" + query;
    }

    public static TokenInfo getTokenInfo(String accessToken) throws Exception {

        URI endpointUri = new URI(Conf.getInstance().getASAccessTokenValidationUri());
        TokenValidator tokenValidator = new TokenValidator(endpointUri,
                Conf.getInstance().getClientKeystoreFile(), Conf.getInstance().getClientKeystorePass(),
                Conf.getInstance().getClientTruststoreFile(), Conf.getInstance().getClientTruststorePass());

        return tokenValidator.checkToken(accessToken, "oauth-java-client-demo");
    }
}

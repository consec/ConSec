package org.ow2.contrail.common.oauth.client.atmanager;

import org.ow2.contrail.common.oauth.client.AccessToken;
import org.ow2.contrail.common.oauth.client.exceptions.UnauthorizedException;

public interface OAuthATManager {

    public AccessToken getAccessToken(String resourceOwnerUuid) throws UnauthorizedException, Exception;

}

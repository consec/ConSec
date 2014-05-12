package org.ow2.contrail.common.oauth.client.utils;

import java.net.URI;
import java.net.URISyntaxException;

public class UriUtils {

    public static URI append (URI baseUri, String path) throws URISyntaxException {
        String url = baseUri.toString();
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += (path.startsWith("/")) ? path.substring(1) : path;
        return new URI(url);
    }
}

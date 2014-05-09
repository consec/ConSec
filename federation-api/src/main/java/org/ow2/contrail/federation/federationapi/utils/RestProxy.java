package org.ow2.contrail.federation.federationapi.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.ow2.contrail.common.oauth.client.OAuthFilter;
import org.ow2.contrail.common.oauth.client.OAuthHttpClient;
import org.ow2.contrail.common.oauth.client.TokenInfo;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestProxy {
    private OAuthHttpClient oAuthHttpClient;
    private URI targetUri;
    private Map<String, String> rewriteRules;
    private Map<String, String> locationHeaderRewriteRules;

    public RestProxy(URI targetUri, Map<String, String> rewriteRules, Map<String, String> locationHeaderRewriteRules) {
        this.targetUri = targetUri;
        this.rewriteRules = rewriteRules;
        this.locationHeaderRewriteRules = locationHeaderRewriteRules;
        oAuthHttpClient = new OAuthHttpClient(
                Conf.getInstance().getOAuthClientKeystoreFile(),
                Conf.getInstance().getOAuthClientKeystorePass(),
                Conf.getInstance().getOAuthClientTruststoreFile(),
                Conf.getInstance().getOAuthClientTruststorePass()
        );
    }

    public Response forward(String path, HttpServletRequest httpRequest) throws Exception {
        TokenInfo tokenInfo = OAuthFilter.getAccessTokenInfo(httpRequest);
        if (tokenInfo == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("OAuth access token is missing.")
                            .build());
        }
        String accessToken = tokenInfo.getAccessToken();

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        URI uri = targetUri.resolve(path);

        String method = httpRequest.getMethod();
        HttpResponse httpResponse;

        if (method.equals("GET")) {
            if (httpRequest.getQueryString() != null) {
                uri = new URI(uri.toString() + "?" + httpRequest.getQueryString());
            }
            httpResponse = oAuthHttpClient.get(uri, accessToken);
        }
        else if (method.equals("POST")) {
            String requestContent = getContent(httpRequest.getInputStream());

            Pattern pattern = Pattern.compile("([\\w+-/]+); charset=([\\w-]+)");
            Matcher m = pattern.matcher(httpRequest.getContentType());
            if (!m.find()) {
                throw new Exception("Unsupported Content-Type header: " + httpRequest.getContentType());
            }

            String mimeType = m.group(1);
            String charset = m.group(2);
            ContentType contentType = ContentType.create(mimeType, charset);

            HttpEntity entity = new StringEntity(requestContent, contentType);
            httpResponse = oAuthHttpClient.post(uri, accessToken, entity);
        }
        else if (method.equals("PUT")) {
            String requestContent = getContent(httpRequest.getInputStream());

            Pattern pattern = Pattern.compile("([\\w+-/]+); charset=([\\w-]+)");
            Matcher m = pattern.matcher(httpRequest.getContentType());
            if (!m.find()) {
                throw new Exception("Invalid Content-Type header: " + httpRequest.getContentType());
            }

            String mediaType = m.group(1);
            String charset = m.group(2);
            ContentType contentType = ContentType.create(mediaType, charset);

            HttpEntity entity = new StringEntity(requestContent, contentType);
            httpResponse = oAuthHttpClient.put(uri, accessToken, entity);
        }
        else if (method.equals("DELETE")) {
            httpResponse = oAuthHttpClient.delete(uri, accessToken);
        }
        else {
            throw new Exception("Unsupported HTTP method: " + method);
        }

        Response.ResponseBuilder builder = Response
                .status(httpResponse.getStatusLine().getStatusCode());

        if (httpResponse.getEntity() != null && httpResponse.getEntity().getContent() != null) {
            String responseContent = getContent(httpResponse.getEntity().getContent());
            String contentType = httpResponse.getEntity().getContentType().getValue();
            String[] contentTypeArr = contentType.split("; ?");
            String mediaType = contentTypeArr[0];

            // rewrite URIs in json
            if (httpResponse.getStatusLine().getStatusCode() == 200 &&
                    mediaType.equals("application/json")) {
                responseContent = rewrite(responseContent, rewriteRules);
            }

            byte[] responseBytes = responseContent.getBytes();
            builder = builder.entity(responseBytes)
                    .type(contentType);

            builder.header("Content-Length", responseBytes.length);
        }

        if (httpResponse.getFirstHeader("Location") != null) {
            String location = rewrite(httpResponse.getFirstHeader("Location").getValue(), locationHeaderRewriteRules);
            builder.header("Location", location);
        }

        return builder.build();
    }

    private String getContent(InputStream is) throws IOException {
        Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }


    private String rewrite(String content, Map<String, String> rewriteRules) {
        if (rewriteRules == null || rewriteRules.size() == 0) {
            return content;
        }

        List<String> searchList = new ArrayList<String>();
        List<String> replacementList = new ArrayList<String>();
        for (Map.Entry<String, String> entry : rewriteRules.entrySet()) {
            searchList.add(entry.getKey());
            replacementList.add(entry.getValue());
        }

        return StringUtils.replaceEach(content,
                searchList.toArray(new String[0]), replacementList.toArray(new String[0]));
    }
}

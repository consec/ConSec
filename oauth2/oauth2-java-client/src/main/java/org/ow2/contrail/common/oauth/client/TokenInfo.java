package org.ow2.contrail.common.oauth.client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TokenInfo {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private String accessToken;
    private String clientId;
    private Date expireTime;
    private long expiresIn;
    private TokenType tokenType;
    private String ownerUuid;
    private List<String> scope;

    public TokenInfo(String json) throws JSONException, ParseException {
        JSONObject o = new JSONObject(json);
        accessToken = o.getString("access_token");
        clientId = o.getString("client_id");
        expireTime = sdf.parse(o.getString("expire_time"));
        expiresIn = o.getLong("expires_in");
        tokenType = TokenType.fromString(o.getString("token_type"));
        ownerUuid = o.getString("owner_uuid");

        JSONArray scopeArr = o.getJSONArray("scope");
        scope = new ArrayList<String>();
        for (int i=0; i<scopeArr.length(); i++) {
            scope.add(scopeArr.getString(i));
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("access_token", accessToken);
        o.put("client_id", clientId);
        o.put("expire_time", sdf.format(expireTime));
        o.put("expires_in", expiresIn);
        o.put("token_type", tokenType.name());
        o.put("owner_uuid", ownerUuid);

        JSONArray scopeArr = new JSONArray();
        for (String scopeItem : scope) {
            scopeArr.put(scopeItem);
        }
        o.put("scope", scopeArr);
        return o;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(String ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public enum TokenType {
        BEARER;

        public static TokenType fromString(String value) {
            return TokenType.valueOf(value.toUpperCase());
        }
    }
}

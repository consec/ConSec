package org.ow2.contrail.common.oauth.client;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class AccessToken {
    private String value;
    private String tokenType;
    private Date expireTime;
    private int expiresIn;

    public AccessToken() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("value", value);
        o.put("tokenType", tokenType);
        o.put("expireTime", expireTime);
        o.put("expiresIn", expiresIn);
        return o.toString();
    }
}

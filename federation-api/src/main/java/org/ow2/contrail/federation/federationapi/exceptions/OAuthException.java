package org.ow2.contrail.federation.federationapi.exceptions;

public class OAuthException extends Exception {
    public OAuthException(String s) {
        super(s);
    }

    public OAuthException(String s, Throwable throwable) {
        super(s, throwable);
    }
}

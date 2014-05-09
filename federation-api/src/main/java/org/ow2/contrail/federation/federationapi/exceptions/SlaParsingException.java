package org.ow2.contrail.federation.federationapi.exceptions;

public class SlaParsingException extends Exception {
    public SlaParsingException(String s) {
        super(s);
    }

    public SlaParsingException(String s, Throwable throwable) {
        super(s, throwable);
    }
}

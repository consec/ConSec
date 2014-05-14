/**
 *
 */
package org.ow2.contrail.federation.federationapi.utils;

/**
 * Class for handling FederationAPI Exceptions.
 *
 * @author ales
 */
public class FederationAPIException extends Exception {

    public static final String FEDERATION_CORE_NOT_ENABLED = "Federation Core is not enabled.";

    /**
     *
     */
    public FederationAPIException() {
    }

    /**
     * @param message
     */
    public FederationAPIException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public FederationAPIException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public FederationAPIException(String message, Throwable cause) {
        super(message, cause);
    }

}

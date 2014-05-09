package org.ow2.contrail.federation.federationapi.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class RestApiException extends WebApplicationException {
    public RestApiException(Response.Status status, String message) {
        super(Response.status(status)
                .entity(message)
                .type(MediaType.TEXT_PLAIN)
                .build());
    }
}

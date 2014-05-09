package org.ow2.contrail.federation.federationapi.resources;

import org.apache.commons.io.IOUtils;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JsonMessageBodyReader implements MessageBodyReader<JSONObject> {

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return aClass.equals(JSONObject.class);
    }

    public JSONObject readFrom(java.lang.Class<JSONObject> type,
                               java.lang.reflect.Type genericType, java.lang.annotation.Annotation[]
                    annotations, MediaType mediaType, MultivaluedMap<
                    String, String> httpHeaders, java.io.InputStream
                    entityStream) throws IOException {
        try {
            String content = IOUtils.toString(entityStream);
            return new JSONObject(content);
        }
        catch (Exception e) {
            String message = "JSON parsing error: " + e.getMessage();
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(message).build());
        }
    }
}

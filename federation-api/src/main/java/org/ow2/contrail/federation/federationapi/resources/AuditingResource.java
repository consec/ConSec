package org.ow2.contrail.federation.federationapi.resources;

import org.ow2.contrail.federation.federationapi.utils.Conf;
import org.ow2.contrail.federation.federationapi.utils.RestProxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Path("/auditing")
public class AuditingResource {
    private URI baseUri;

    public AuditingResource() throws URISyntaxException {
        baseUri = Conf.getInstance().getAddressAuditManager();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("audit_events")
    public Response submitAuditEventsGathererJob(@Context HttpServletRequest httpServletRequest) throws Exception {
        Map<String, String> locationHeaderRewriteRules = new HashMap<String, String>();
        locationHeaderRewriteRules.put(baseUri + "audit_events/", "/auditing/audit_events/");
        RestProxy restProxy = new RestProxy(baseUri, null, locationHeaderRewriteRules);
        return restProxy.forward("/audit_events", httpServletRequest);
    }

    @GET
    @Path("audit_events/reports/{jobId}")
    public Response getStatus(@PathParam("jobId") String jobId, @Context HttpServletRequest httpServletRequest)
            throws Exception {
        Map<String, String> rewriteRules = new HashMap<String, String>();
        rewriteRules.put("\"\\/audit_events\\/", "\"/auditing/audit_events/");
        RestProxy restProxy = new RestProxy(baseUri, rewriteRules, null);
        return restProxy.forward(String.format("/audit_events/reports/%s", jobId), httpServletRequest);
    }

    @GET
    @Path("audit_events/reports/{jobId}/content")
    public Response getReport(@PathParam("jobId") String jobId, @Context HttpServletRequest httpServletRequest)
            throws Exception {
        RestProxy restProxy = new RestProxy(baseUri, null, null);
        return restProxy.forward(String.format("/audit_events/reports/%s/content", jobId), httpServletRequest);
    }
}

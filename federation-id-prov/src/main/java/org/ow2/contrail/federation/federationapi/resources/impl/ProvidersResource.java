package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.IProviderResource;
import org.ow2.contrail.federation.federationapi.resources.IProvidersResource;
import org.ow2.contrail.federation.federationapi.utils.Authorizer;
import org.ow2.contrail.federation.federationapi.utils.DBUtils;
import org.ow2.contrail.federation.federationdb.jpa.dao.ProviderDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.Provider;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Path("/providers")
public class ProvidersResource implements IProvidersResource {
    private static String rootUri = "/providers";
    private static Logger log = Logger.getLogger(ProvidersResource.class);

    @Context
    HttpServletRequest request;

    /**
     * Returns list of all providers the current user has access to.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response getProviders() throws JSONException {
        log.trace("getProviders() started.");

        if (!Authorizer.isAuthorized(request)) {
            throw new WebApplicationException((Response.Status.FORBIDDEN));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        try {
            Query query = em.createNamedQuery("Provider.findAll");
            List<Provider> providerList = query.getResultList();

            JSONArray jsonArray = new JSONArray();
            for (Provider provider : providerList) {
                String uri = String.format("%s/%d", rootUri, provider.getProviderId());
                JSONObject o = new JSONObject();
                o.put("name", provider.getName());
                o.put("uri", uri);
                jsonArray.put(o);
            }

            return Response.ok(jsonArray.toString()).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Creates a new provider.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    public Response addProvider(String requestBody) throws JSONException, URISyntaxException {
        if (log.isTraceEnabled()) {
            log.trace("addProvider() started. Data: " + requestBody);
        }

        if (!Authorizer.isAuthorized(request)) {
            throw new WebApplicationException((Response.Status.FORBIDDEN));
        }

        JSONObject json = new JSONObject(requestBody);
        Provider provider = new Provider(json);

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();
            em.persist(provider);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", provider.getProviderId()));
            return Response.created(resourceUri).build();
        }
        catch (RollbackException e) {
            if (DBUtils.isIntegrityConstraintException(e)) {
                return Response.status(Response.Status.CONFLICT).build();
            }
            else {
                throw e;
            }
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param providerId
     * @return
     */
    @Path("/{providerId}")
    public IProviderResource findProvider(@PathParam("providerId") int providerId) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("findProvider(ID=%d) started.", providerId));
        }

        if (!Authorizer.isAuthorized(request)) {
            throw new WebApplicationException((Response.Status.FORBIDDEN));
        }

        Provider provider = ProviderDAO.findById(providerId);
        if (provider == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new ProviderResource(provider);
        }
    }

}

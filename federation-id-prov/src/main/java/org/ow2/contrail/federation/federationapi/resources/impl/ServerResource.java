package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.IServerResource;
import org.ow2.contrail.federation.federationdb.jpa.entities.Provider;
import org.ow2.contrail.federation.federationdb.jpa.entities.Server;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public class ServerResource implements IServerResource {
    private static Logger log = Logger.getLogger(ServerResource.class);
    private Provider provider;
    private Server server;
    private String rootUri;

    public ServerResource(Provider provider, Server server) {
        this.provider = provider;
        this.server = server;
        rootUri = String.format("/providers/%d/servers/%d", provider.getProviderId(), server.getServerId());
    }

    /**
     * Returns the JSON representation of the given server.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response getServer() throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("getServer(ID=%d) started.", server.getServerId()));
        }

        JSONObject json = server.toJSON();
        json.put("uri", rootUri);

        return Response.ok(json.toString()).build();
    }

    /**
     * Updates the selected server.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    public Response updateServer(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("updateServer(ID=%d) started. Data: %s", server.getServerId(), requestBody));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONObject json = new JSONObject(requestBody);
            server.update(json);

            em.getTransaction().begin();
            em.merge(server);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        catch (Exception e) {
            log.error("Update failed: ", e);
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).
                            entity(String.format("Update failed: %s.", e.getMessage())).
                            build()
            );
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Deletes selected server.
     *
     * @return
     */
    @DELETE
    public Response removeServer() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("removeServer(ID=%d) started.", server.getServerId()));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            server = em.merge(server);
            em.remove(server);
            provider.getServerList().remove(server);
            provider = em.merge(provider);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }
}

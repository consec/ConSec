package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.ICEEResource;
import org.ow2.contrail.federation.federationapi.resources.IEDCResource;
import org.ow2.contrail.federation.federationapi.resources.IVirtualOrganizationResource;
import org.ow2.contrail.federation.federationdb.jpa.dao.ClusterDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.Cluster;
import org.ow2.contrail.federation.federationdb.jpa.entities.Provider;
import org.ow2.contrail.federation.federationdb.jpa.entities.Vo;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VirtualOrganizationResource implements IVirtualOrganizationResource {
    private static Logger log = Logger.getLogger(VirtualOrganizationResource.class);
    private Provider provider;
    private Vo vo;
    private String rootUri;

    public VirtualOrganizationResource(Provider provider, Vo vo) {
        this.provider = provider;
        this.vo = vo;
        this.rootUri = String.format("/providers/%d/vos/%d", provider.getProviderId(), vo.getVoId());
    }

    /**
     * Returns the JSON representation of the provider's virtual organization.
     *
     * @return
     */
    @Override
    @GET
    @Produces("application/json")
    public Response getVO() throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("getVO(ID=%d) started.", vo.getVoId()));
        }

        JSONObject json = vo.toJSON();

        json.put("uri", rootUri);
        json.put("clusters", rootUri + "/clusters");

        log.trace("getVO() finished successfully.");
        return Response.ok(json.toString()).build();
    }

    /**
     * Updates the selected VO.
     *
     * @return
     */
    @Override
    @PUT
    @Consumes("application/json")
    public Response updateVO(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("updateVO(ID=%d) started. Data: %s", vo.getVoId(), requestBody));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONObject json = new JSONObject(requestBody);
            vo.update(json);

            em.getTransaction().begin();
            em.merge(vo);
            em.getTransaction().commit();

            log.trace("updateVO() finished successfully.");
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
     * Deletes selected VO.
     *
     * @return
     */
    @Override
    @DELETE
    public Response removeVO() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("removeVO(ID=%d) started.", vo.getVoId()));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            vo = em.merge(vo);
            em.remove(vo);
            provider.getVoList().remove(vo);
            provider = em.merge(provider);
            em.getTransaction().commit();

            log.trace("removeVO() finished successfully.");
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Returns all clusters registered at the given VO.
     *
     * @return
     */
    @Override
    @GET
    @Path("/clusters")
    @Produces("application/json")
    public Response getClusters() throws Exception {
        log.trace("getClusters() started.");
        JSONArray json = new JSONArray();
        for (Cluster cluster : vo.getClusterList()) {
            JSONObject o = new JSONObject();
            String uri = String.format("%s/clusters/%d", rootUri, cluster.getClusterId());
            String clusterUri = String.format("/providers/%d/clusters/%d", provider.getProviderId(), cluster.getClusterId());
            o.put("uri", uri);
            o.put("baseUri", clusterUri);
            json.put(o);
        }

        log.trace("getClusters() finished successfully.");
        return Response.ok(json.toString()).build();
    }

    /**
     * Registers cluster at the selected VO.
     *
     * @return
     */
    @Override
    @POST
    @Path("/clusters")
    @Consumes("application/json")
    public Response registerCluster(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("registerCluster() started. Data: " + requestBody);
        }

        Cluster cluster;
        try {
            JSONObject json = new JSONObject(requestBody);
            String clusterURI = json.getString("clusterURI");
            Pattern uriPattern = Pattern.compile(
                    String.format("^/providers/%d/clusters/(\\d+)$", provider.getProviderId()));
            Matcher m = uriPattern.matcher(clusterURI);
            if (!m.find()) {
                throw new Exception("Invalid cluster URI: " + clusterURI);
            }
            int clusterId = Integer.parseInt(m.group(1));
            cluster = ClusterDAO.findById(provider, clusterId);
            if (cluster == null) {
                throw new Exception(String.format("Cluster '%s' not found.", clusterURI));
            }
        }
        catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        try {
            if (!vo.getClusterList().contains(cluster)) {
                em.getTransaction().begin();
                vo.getClusterList().add(cluster);
                cluster.getVoList().add(vo);
                cluster = em.merge(cluster);
                vo = em.merge(vo);
                em.getTransaction().commit();
            }
            // no problem if cluster is already registered

            URI resourceUri = new URI(String.format("/%d", cluster.getClusterId()));
            log.trace("registerCluster() finished successfully.");
            return Response.created(resourceUri).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Returns data about specific cluster registration
     *
     * @return
     */
    @Override
    @GET
    @Path("/clusters/{id}")
    @Produces("application/json")
    public Response getClusterRegistration(@PathParam("id") int id) throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("getClusterRegistration(%d) started.", id));
        }
        for (Cluster cluster : vo.getClusterList()) {
            if (cluster.getClusterId() == id) {
                JSONObject o = new JSONObject();
                String uri = String.format("%s/clusters/%d", rootUri, cluster.getClusterId());
                String clusterUri = String.format("/providers/%d/clusters/%d", provider.getProviderId(), cluster.getClusterId());
                o.put("uri", uri);
                o.put("baseUri", clusterUri);
                log.trace("getClusterRegistration() finished successfully. Cluster registration found.");
                return Response.ok(o.toString()).build();
            }
        }

        log.trace("Cluster is not registered at given VO.");
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    /**
     * Unregisters given cluster from given VO.
     *
     * @return
     */
    @Override
    @DELETE
    @Path("/clusters/{id}")
    @Produces("application/json")
    public Response unregisterCluster(@PathParam("id") int id) throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("unregisterCluster(%d) started.", id));
        }

        for (Cluster cluster : vo.getClusterList()) {
            if (cluster.getClusterId() == id) {
                EntityManager em = PersistenceUtils.getInstance().getEntityManager();
                try {
                    em.getTransaction().begin();
                    vo.getClusterList().remove(cluster);
                    cluster.getVoList().remove(vo);
                    vo = em.merge(vo);
                    cluster = em.merge(cluster);
                    em.getTransaction().commit();

                    return Response.status(Response.Status.NO_CONTENT).build();
                }
                finally {
                    PersistenceUtils.getInstance().closeEntityManager(em);
                }
            }
        }

        log.trace("Cluster is not registered at given VO.");
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    /**
     * Returns a list of all CEEs for the selected provider and virtual organization.
     *
     * @return
     */
    @Override
    @GET
    @Path("/cees")
    @Produces("application/json")
    public Response getCEEs
    () {
        String json = String.format("Provider %d VO %d CEEs.", provider.getProviderId(), vo.getVoId());
        return Response.ok(json).build();
    }

    /**
     * Creates a new CEE for the selected provider and virtual organization.
     *
     * @return
     */
    @Override
    @POST
    @Path("/cees")
    public Response createCEE() throws Exception {
        URI resourceUri = new URI("/1");
        return Response.created(resourceUri).build();
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param ceeId
     * @return
     */
    @Override
    @Path("/cees/{ceeId}")
    public ICEEResource findCEE(@PathParam("ceeId") int ceeId) {
        return new CEEResource(provider.getProviderId(), vo.getVoId(), ceeId);
    }

    /**
     * Returns a list of elastic DCs for the provider's VO.
     *
     * @return
     */
    @Override
    @GET
    @Path("/edcs")
    @Produces("application/json")
    public Response getEDCs() {
        String json = String.format("Provider %d VO %d EDCs.", provider.getProviderId(), vo.getVoId());
        return Response.ok(json).build();
    }

    /**
     * Creates a new EDC for the selected provider.
     *
     * @return
     */
    @Override
    @POST
    @Path("/edcs")
    public Response createEDC() throws Exception {
        URI resourceUri = new URI("/1");
        return Response.created(resourceUri).build();
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param edcId
     * @return
     */
    @Override
    @Path("/edcs/{edcId}")
    public IEDCResource findEDC(
            @PathParam("edcId") int edcId) {
        return new EDCResource(provider.getProviderId(), vo.getVoId(), edcId);
    }

    /**
     * Returns all attributes of the provider's VO.
     *
     * @return
     */
    @Override
    @GET
    @Path("/attributes")
    @Produces("application/json")
    public Response getAttributes() {
        String json = String.format("Provider %d VO %d attributes.", provider.getProviderId(), vo.getVoId());
        return Response.ok(json).build();
    }

    /**
     * Updates attributes of the provider's VO.
     *
     * @return
     */
    @Override
    @PUT
    @Path("/attributes")
    public Response updateAttributes() throws Exception {
        return Response.ok().build();
    }
}

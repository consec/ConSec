package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.*;
import org.ow2.contrail.federation.federationapi.utils.DBUtils;
import org.ow2.contrail.federation.federationdb.jpa.dao.*;
import org.ow2.contrail.federation.federationdb.jpa.entities.*;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;

public class ProviderResource implements IProviderResource {
    private static Logger log = Logger.getLogger(ProviderResource.class);
    private Provider provider;
    private String rootUri;

    public ProviderResource(Provider provider) {
        this.provider = provider;
        rootUri = String.format("/providers/%d", provider.getProviderId());
    }

    /**
     * Returns the JSON representation of the provider.
     *
     * @return
     */
    @Override
    @GET
    @Produces("application/json")
    public Response getProvider() throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("getProvider(ID=%d) started.", provider.getProviderId()));
        }

        JSONObject json = provider.toJSON();

        json.put("uri", rootUri);
        json.put("servers", rootUri + "/servers");
        json.put("vms", rootUri + "/vms");
        json.put("clusters", rootUri + "/clusters");
        json.put("ovfs", rootUri + "/ovfs");
        json.put("slats", rootUri + "/slats");

        return Response.ok(json.toString()).build();
    }

    /**
     * Updates the selected provider.
     *
     * @return
     */
    @Override
    @PUT
    @Consumes("application/json")
    public Response updateProvider(String requestBody) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("updateProvider(ID=%d) started. Data: %s", provider.getProviderId(), requestBody));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONObject json = new JSONObject(requestBody);
            provider.update(json);

            em.getTransaction().begin();
            em.merge(provider);
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
     * Deletes selected provider.
     *
     * @return
     */
    @Override
    @DELETE
    public Response removeProvider() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("removeProvider(ID=%d) started.", provider.getProviderId()));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            provider = em.merge(provider);
            em.remove(provider);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Return a list of all virtual organizations for the provider.
     *
     * @return
     */
    @Override
    @GET
    @Path("/vos")
    @Produces("application/json")
    public Response getVOs() throws Exception {
        log.trace("getVOs() started.");

        JSONArray json = new JSONArray();
        for (Vo vo : provider.getVoList()) {
            String uri = String.format("%s/vos/%d", rootUri, vo.getVoId());
            JSONObject o = new JSONObject();
            o.put("name", vo.getName());
            o.put("uri", uri);
            json.put(o);
        }

        return Response.ok(json.toString()).build();
    }

    /**
     * Creates a new virtual organization for the provider.
     *
     * @return
     */
    @Override
    @POST
    @Path("/vos")
    @Consumes("application/json")
    public Response addVO(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("addVO() started. Data: " + requestBody);
        }

        Vo vo = null;
        try {
            JSONObject json = new JSONObject(requestBody);
            vo = new Vo(json);
            vo.setProviderId(provider);
        }
        catch (JSONException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).
                            entity("Invalid JSON data: " + e.getMessage()).build()
            );
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();
            em.persist(vo);
            provider.getVoList().add(vo);
            provider = em.merge(provider);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", vo.getVoId()));
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
     * @param voId
     * @return
     */
    @Override
    @Path("/vos/{voId}")
    public IVirtualOrganizationResource findVO(@PathParam("voId") int voId) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("findVO(ID=%d) started.", voId));
        }

        Vo vo = VoDAO.findById(voId);
        if (vo == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new VirtualOrganizationResource(provider, vo);
        }
    }

    /**
     * Return a list of all users for the selected provider.
     *
     * @return
     */
    @Override
    @GET
    @Path("/users")
    @Produces("application/json")
    public Response getUsers() {
        String json = String.format("Provider %d users", provider.getProviderId());
        return Response.ok(json).build();
    }

    /**
     * Links new user with the provider or updates existing one.
     *
     * @return
     */
    @Override
    @POST
    @Path("/users")
    public Response addUser() throws Exception {
        URI resourceUri = new URI("/1");
        return Response.created(resourceUri).build();
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param userId
     * @return
     */
    @Override
    @Path("/users/{uid}")
    public IUserResource findUser(@PathParam("uid") int userId) {
        return new UserResource(provider.getProviderId(), userId);
    }

    /**
     * Returns a list of all data centers for the selected provider.
     *
     * @return
     */
    @Override
    @GET
    @Path("/dcs")
    @Produces("application/json")
    public Response getDataCenters() {
        String json = String.format("Provider %d DCs", provider.getProviderId());
        return Response.ok(json).build();
    }

    /**
     * Links new data center with the provider or updates existing one.
     *
     * @return
     */
    @Override
    @POST
    @Path("/dcs")
    public Response addDataCenter() throws Exception {
        URI resourceUri = new URI("/1");
        return Response.created(resourceUri).build();
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param dcId
     * @return
     */
    @Override
    @Path("/dcs/{dcid}")
    public IDataCenterResource findDataCenter(@PathParam("dcid") int dcId) {
        return new DataCenterResource(provider.getProviderId(), dcId);
    }

    /**
     * Returns all provider's virtual machines.
     *
     * @return
     */
    @Override
    @GET
    @Path("/vms")
    @Produces("application/json")
    public Response getVMs() throws Exception {
        log.trace("getVMs() started.");

        JSONArray json = new JSONArray();
        for (Vm vm : provider.getVmList()) {
            String uri = String.format("%s/vms/%d", rootUri, vm.getVmId());
            JSONObject o = new JSONObject();
            o.put("name", vm.getName());
            o.put("uri", uri);
            json.put(o);
        }

        return Response.ok(json.toString()).build();
    }

    /**
     * Creates a new virtual machine for the selected provider.
     *
     * @return
     */
    @Override
    @POST
    @Path("/vms")
    @Consumes("application/json")
    public Response addVM(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("addVM() started. Data: " + requestBody);
        }

        Vm vm = null;
        try {
            JSONObject json = new JSONObject(requestBody);
            vm = new Vm(json);
            vm.setProviderId(provider);
        }
        catch (JSONException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).
                            entity("Invalid JSON data: " + e.getMessage()).build()
            );
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();
            em.persist(vm);
            provider.getVmList().add(vm);
            provider = em.merge(provider);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", vm.getVmId()));
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
     * @param vmId
     * @return
     */
    @Override
    @Path("/vms/{vmid}")
    public IVmResource findVm(@PathParam("vmid") int vmId) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("findVm(ID=%d) started.", vmId));
        }

        Vm vm = VmDAO.findById(provider, vmId);
        if (vm == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new VmResource(provider, vm);
        }
    }

    /**
     * Returns all storages of the selected provider.
     *
     * @return
     */
    @Override
    @GET
    @Path("/storages")
    @Produces("application/json")
    public Response getStorages() {
        String json = String.format("Provider %d storages.", provider.getProviderId());
        return Response.ok(json).build();
    }

    /**
     * Creates a new storage for the selected provider.
     *
     * @return
     */
    @Override
    @POST
    @Path("/storages")
    public Response createStorage() throws Exception {
        URI resourceUri = new URI("/1");
        return Response.created(resourceUri).build();
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param storageId
     * @return
     */
    @Override
    @Path("/storages/{sid}")
    public IStorageResource findStorage(@PathParam("sid") int storageId) {
        return new StorageResource(provider.getProviderId(), storageId);
    }

    /**
     * Returns all networks of the selected provider.
     *
     * @return
     */
    @Override
    @GET
    @Path("/networks")
    @Produces("application/json")
    public Response getNetworks() {
        String json = String.format("Provider %d networks.", provider.getProviderId());
        return Response.ok(json).build();
    }

    /**
     * Creates a new network for the selected provider.
     *
     * @return
     */
    @Override
    @POST
    @Path("/networks")
    public Response createNetwork() throws Exception {
        URI resourceUri = new URI("/1");
        return Response.created(resourceUri).build();
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param networkId
     * @return
     */
    @Override
    @Path("/networks/{nid}")
    public INetworkResource findNetwork(@PathParam("nid") int networkId) {
        return new NetworkResource(provider.getProviderId(), networkId);
    }

    /**
     * Returns all provider's servers.
     *
     * @return
     */
    @Override
    @GET
    @Path("/servers")
    @Produces("application/json")
    public Response getServers() throws Exception {
        log.trace("getServers() started.");

        JSONArray json = new JSONArray();
        for (Server server : provider.getServerList()) {
            String uri = String.format("%s/servers/%d", rootUri, server.getServerId());
            JSONObject o = new JSONObject();
            o.put("name", server.getName());
            o.put("uri", uri);
            json.put(o);
        }

        return Response.ok(json.toString()).build();
    }

    /**
     * Creates a new server for the given provider.
     *
     * @return
     */
    @Override
    @POST
    @Path("/servers")
    @Consumes("application/json")
    public Response addServer(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("addServer() started. Data: " + requestBody);
        }

        Server server = null;
        try {
            JSONObject json = new JSONObject(requestBody);
            server = new Server(json);
            server.setProviderId(provider);
        }
        catch (JSONException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).
                            entity("Invalid JSON data: " + e.getMessage()).build()
            );
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();
            em.persist(server);
            provider.getServerList().add(server);
            provider = em.merge(provider);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", server.getServerId()));
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
     * @param serverId
     * @return
     */
    @Override
    @Path("/servers/{sid}")
    public IServerResource findServer(@PathParam("sid") int serverId) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("findServer(ID=%d) started.", serverId));
        }

        Server server = ServerDAO.findById(provider, serverId);
        if (server == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new ServerResource(provider, server);
        }
    }

    /**
     * Returns all provider's clusters.
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
        for (Cluster cluster : provider.getClusterList()) {
            String uri = String.format("%s/clusters/%d", rootUri, cluster.getClusterId());
            JSONObject o = new JSONObject();
            o.put("name", cluster.getName());
            o.put("uri", uri);
            json.put(o);
        }

        return Response.ok(json.toString()).build();
    }

    /**
     * Creates a new cluster for the selected provider.
     *
     * @return
     */
    @Override
    @POST
    @Path("/clusters")
    @Consumes("application/json")
    public Response addCluster(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("addCluster() started. Data: " + requestBody);
        }

        Cluster cluster = null;
        try {
            JSONObject json = new JSONObject(requestBody);
            cluster = new Cluster(json);
            cluster.setProviderId(provider);
        }
        catch (JSONException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).
                            entity("Invalid JSON data: " + e.getMessage()).build()
            );
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();
            em.persist(cluster);
            provider.getClusterList().add(cluster);
            provider = em.merge(provider);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", cluster.getClusterId()));
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
     * @param clusterId
     * @return
     */
    @Override
    @Path("/clusters/{cid}")
    public IClusterResource findCluster(@PathParam("cid") int clusterId) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("findCluster(ID=%d) started.", clusterId));
        }

        Cluster cluster = ClusterDAO.findById(provider, clusterId);
        if (cluster == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new ClusterResource(provider, cluster);
        }
    }

    /**
     * Returns all provider's OVFs.
     * Due to the move of OVFs under Application, this method is not working
     * properly.
     *
     * @return
     */
    @GET
    @Path("/ovfs")
    @Produces("application/json")
    public Response getOvfs() throws Exception {
        JSONArray json = new JSONArray();
        for (Ovf ovf : provider.getOvfList()) {
            String uri = String.format("/providers/%d/ovfs/%d", provider.getProviderId(),
                    ovf.getOvfId());
            JSONObject o = new JSONObject();
            o.put("name", ovf.getName());
            o.put("uri", uri);
            json.put(o);
        }
        return Response.ok(json.toString()).build();
    }


    /**
     * Creates a new OVF for the given provider.
     * Due to the move of OVFs under Application, this method is not working
     * properly.
     *
     * @return
     */
    @POST
    @Path("/ovfs")
    @Consumes("application/json")
    public Response addOvf(String requestBody) throws Exception {
        JSONObject json = new JSONObject(requestBody);
        String name = (String) json.get("name");

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            // check if OVF is already registered
            Query query = em.createQuery("SELECT COUNT(o) FROM Ovf o WHERE o.name=:name");
            query.setParameter("name", name);

            if ((Long) query.getSingleResult() > 0) {
                // resource is already registered
                return Response.status(Response.Status.CONFLICT).build();
            }

            Ovf ovf = new Ovf();
            ovf.setName(name);
            ovf.setAttributes(json.toString());
            ovf.setProviderId(provider);

            em.getTransaction().begin();
            em.persist(ovf);
            provider.getOvfList().add(ovf);
            provider = em.merge(provider);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", ovf.getOvfId()));
            return Response.created(resourceUri).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param ovfId
     * @return
     */
    @Path("/ovfs/{oid}")
    public OvfResource findOvf(@PathParam("oid") int ovfId) {
        Ovf ovf = OvfDAO.findById(ovfId);
        if (ovf == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new OvfResource(provider, ovf);
        }
    }

    /**
     * Returns a list of all registered SLA templates for the selected provider.
     *
     * @return
     */
    @Override
    @GET
    @Path("/slats")
    @Produces("application/json")
    public Response getSLATemplates() throws Exception {
        log.trace("getSLATemplates() started.");
        JSONArray json = new JSONArray();
        for (SLATemplate slaTemplate : provider.getSLATemplateList()) {
            String uri = String.format(rootUri + "/slats/%d", slaTemplate.getSlatId());
            JSONObject o = new JSONObject();
            o.put("name", slaTemplate.getName());
            o.put("uri", uri);
            o.put("url", slaTemplate.getUrl());
            json.put(o);
        }

        return Response.ok(json.toString()).build();
    }

    /**
     * Registers a new SLA template at the selected provider.
     *
     * @return
     */
    @Override
    @POST
    @Path("/slats")
    @Consumes("application/json")
    public Response addSLATemplate(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("addSLATemplate() started. Data: " + requestBody);
        }

        SLATemplate slaTemplate;
        try {
            JSONObject json = new JSONObject(requestBody);
            slaTemplate = new SLATemplate(json);
            slaTemplate.setProviderId(provider);
        }
        catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).
                            entity("Invalid JSON data: " + e.getMessage()).build()
            );
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();
            em.persist(slaTemplate);
            provider.getSLATemplateList().add(slaTemplate);
            provider = em.merge(provider);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", slaTemplate.getSlatId()));
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
     * @param slatId
     * @return
     */
    @Override
    @Path("/slats/{slatId}")
    public ISLATemplateResource findSLATemplate(@PathParam("slatId") int slatId) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("findSLATemplate(ID=%d) started.", slatId));
        }

        SLATemplate slat = SLATemplateDAO.findById(slatId);
        if (slat == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new SLATemplateResource(provider, slat);
        }
    }
}

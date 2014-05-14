package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.IClusterResource;
import org.ow2.contrail.federation.federationapi.resources.INetworkResource;
import org.ow2.contrail.federation.federationapi.resources.IStorageResource;
import org.ow2.contrail.federation.federationdb.jpa.dao.ServerDAO;
import org.ow2.contrail.federation.federationdb.jpa.dao.VmDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.Cluster;
import org.ow2.contrail.federation.federationdb.jpa.entities.Provider;
import org.ow2.contrail.federation.federationdb.jpa.entities.Server;
import org.ow2.contrail.federation.federationdb.jpa.entities.Vm;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClusterResource implements IClusterResource {
    private static Logger log = Logger.getLogger(ClusterResource.class);
    private Provider provider;
    private int dcId;
    private Cluster cluster;
    private String rootUri;

    public ClusterResource(Provider provider, Cluster cluster) {
        this.provider = provider;
        this.cluster = cluster;
        this.rootUri = String.format("/providers/%d/clusters/%d", provider.getProviderId(), cluster.getClusterId());
    }

    public ClusterResource(int providerId, int dcId, int clusterId) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the JSON representation of the given cluster.
     *
     * @return
     */
    @Override
    @GET
    @Produces("application/json")
    public Response getCluster() throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("getCluster(ID=%d) started.", cluster.getClusterId()));
        }

        JSONObject json = cluster.toJSON();

        json.put("uri", rootUri);
        json.put("servers", rootUri + "/servers");
        json.put("vms", rootUri + "/vms");

        return Response.ok(json.toString()).build();
    }

    /**
     * Updates the given cluster.
     *
     * @return
     */
    @Override
    @PUT
    @Consumes("application/json")
    public Response updateCluster(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("updateCluster(ID=%d) started. Data: %s", cluster.getClusterId(), requestBody));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONObject json = new JSONObject(requestBody);
            cluster.update(json);

            em.getTransaction().begin();
            em.merge(cluster);
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
     * Deletes selected cluster.
     *
     * @return
     */
    @Override
    @DELETE
    public Response removeCluster() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("removeCluster(ID=%d) started.", cluster.getClusterId()));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            cluster = em.merge(cluster);
            em.remove(cluster);
            provider.getClusterList().remove(cluster);
            provider = em.merge(provider);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Returns all servers registered at the given cluster.
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
        for (Server server : cluster.getServerList()) {
            JSONObject o = new JSONObject();
            String uri = String.format("%s/servers/%d", rootUri, server.getServerId());
            String serverUri = String.format("/providers/%d/servers/%d", provider.getProviderId(), server.getServerId());
            o.put("uri", uri);
            o.put("baseUri", serverUri);
            json.put(o);
        }

        log.trace("getServers() finished successfully.");
        return Response.ok(json.toString()).build();
    }

    /**
     * Registers server at the selected cluster.
     *
     * @return
     */
    @Override
    @POST
    @Path("/servers")
    @Consumes("application/json")
    public Response registerServer(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("registerServer() started. Data: " + requestBody);
        }

        Server server;
        try {
            JSONObject json = new JSONObject(requestBody);
            String serverURI = json.getString("serverURI");
            Pattern uriPattern = Pattern.compile(
                    String.format("^/providers/%d/servers/(\\d+)$", provider.getProviderId()));
            Matcher m = uriPattern.matcher(serverURI);
            if (!m.find()) {
                throw new Exception("Invalid server URI: " + serverURI);
            }
            int serverId = Integer.parseInt(m.group(1));
            server = ServerDAO.findById(provider, serverId);
            if (server == null) {
                throw new Exception(String.format("Server '%s' not found.", serverURI));
            }
        }
        catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        try {
            if (!cluster.getServerList().contains(server)) {
                em.getTransaction().begin();
                server.getClusterList().add(cluster);
                cluster.getServerList().add(server);
                server = em.merge(server);
                cluster = em.merge(cluster);
                em.getTransaction().commit();
            }
            // no problem if server is already registered

            URI resourceUri = new URI(String.format("/%d", server.getServerId()));
            log.trace("registerServer() finished successfully.");
            return Response.created(resourceUri).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Returns data about specific server registration
     *
     * @return
     */
    @Override
    @GET
    @Path("/servers/{id}")
    @Produces("application/json")
    public Response getServerRegistration(@PathParam("id") int id) throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("getServerRegistration(%d) started.", id));
        }
        for (Server server : cluster.getServerList()) {
            if (server.getServerId() == id) {
                JSONObject o = new JSONObject();
                String uri = String.format("%s/servers/%d", rootUri, server.getServerId());
                String serverUri = String.format("/providers/%d/servers/%d", provider.getProviderId(), server.getServerId());
                o.put("uri", uri);
                o.put("baseUri", serverUri);
                log.trace("getServerRegistration() finished successfully. Server registration found.");
                return Response.ok(o.toString()).build();
            }
        }

        log.trace("Server is not registered at given cluster.");
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    /**
     * Unregisters given server from given cluster.
     *
     * @return
     */
    @Override
    @DELETE
    @Path("/servers/{id}")
    @Produces("application/json")
    public Response unregisterServer(@PathParam("id") int id) throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("unregisterServer(%d) started.", id));
        }

        for (Server server : cluster.getServerList()) {
            if (server.getServerId() == id) {
                EntityManager em = PersistenceUtils.getInstance().getEntityManager();
                try {
                    em.getTransaction().begin();
                    server.getClusterList().remove(cluster);
                    cluster.getServerList().remove(server);
                    server = em.merge(server);
                    cluster = em.merge(cluster);
                    em.getTransaction().commit();

                    return Response.status(Response.Status.NO_CONTENT).build();
                }
                finally {
                    PersistenceUtils.getInstance().closeEntityManager(em);
                }
            }
        }

        log.trace("Server is not registered at given cluster.");
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    /**
     * Returns all VMs registered at the given cluster.
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
        for (Vm vm : cluster.getVmList()) {
            JSONObject o = new JSONObject();
            String uri = String.format("%s/vms/%d", rootUri, vm.getVmId());
            String vmUri = String.format("/providers/%d/vms/%d", provider.getProviderId(), vm.getVmId());
            o.put("uri", uri);
            o.put("baseUri", vmUri);
            json.put(o);
        }

        log.trace("getVMs() finished successfully.");
        return Response.ok(json.toString()).build();
    }

    /**
     * Registers VM at the selected cluster.
     *
     * @return
     */
    @Override
    @POST
    @Path("/vms")
    @Consumes("application/json")
    public Response registerVM(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("registerVM() started. Data: " + requestBody);
        }

        Vm vm;
        try {
            JSONObject json = new JSONObject(requestBody);
            String vmURI = json.getString("vmURI");
            Pattern uriPattern = Pattern.compile(
                    String.format("^/providers/%d/vms/(\\d+)$", provider.getProviderId()));
            Matcher m = uriPattern.matcher(vmURI);
            if (!m.find()) {
                throw new Exception("Invalid VM URI: " + vmURI);
            }
            int vmId = Integer.parseInt(m.group(1));
            vm = VmDAO.findById(provider, vmId);
            if (vm == null) {
                throw new Exception(String.format("VM '%s' not found.", vmURI));
            }
        }
        catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();

        try {
            if (!cluster.getVmList().contains(vm)) {
                em.getTransaction().begin();
                vm.getClusterList().add(cluster);
                cluster.getVmList().add(vm);
                vm = em.merge(vm);
                cluster = em.merge(cluster);
                em.getTransaction().commit();
            }
            // no problem if VM is already registered

            URI resourceUri = new URI(String.format("/%d", vm.getVmId()));
            log.trace("registerVM() finished successfully.");
            return Response.created(resourceUri).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Returns data about specific VM registration
     *
     * @return
     */
    @Override
    @GET
    @Path("/vms/{id}")
    @Produces("application/json")
    public Response getVMRegistration(@PathParam("id") int id) throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("getVMRegistration(%d) started.", id));
        }
        for (Vm vm : cluster.getVmList()) {
            if (vm.getVmId() == id) {
                JSONObject o = new JSONObject();
                String uri = String.format("%s/vms/%d", rootUri, vm.getVmId());
                String vmUri = String.format("/providers/%d/vms/%d", provider.getProviderId(), vm.getVmId());
                o.put("uri", uri);
                o.put("baseUri", vmUri);
                log.trace("getVMRegistration() finished successfully. VM registration found.");
                return Response.ok(o.toString()).build();
            }
        }

        log.trace("VM is not registered at given cluster.");
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    /**
     * Unregisters given VM from given cluster.
     *
     * @return
     */
    @Override
    @DELETE
    @Path("/vms/{id}")
    @Produces("application/json")
    public Response unregisterVM(@PathParam("id") int id) throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("unregisterVM(%d) started.", id));
        }

        for (Vm vm : cluster.getVmList()) {
            if (vm.getVmId() == id) {
                EntityManager em = PersistenceUtils.getInstance().getEntityManager();
                try {
                    em.getTransaction().begin();
                    vm.getClusterList().remove(cluster);
                    cluster.getVmList().remove(vm);
                    vm = em.merge(vm);
                    cluster = em.merge(cluster);
                    em.getTransaction().commit();

                    return Response.status(Response.Status.NO_CONTENT).build();
                }
                finally {
                    PersistenceUtils.getInstance().closeEntityManager(em);
                }
            }
        }

        log.trace("VM is not registered at given cluster.");
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    /**
     * Returns all storages of the selected cluster.
     *
     * @return
     */
    @Override
    @GET
    @Path("/storages")
    @Produces("application/json")
    public Response getStorages() {
        String json = String.format("Provider %d cluster %d storages.", provider.getProviderId(), cluster.getClusterId());
        return Response.ok(json).build();
    }

    /**
     * Creates a new storage for the selected cluster.
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
        return new StorageResource(provider.getProviderId(), cluster.getClusterId(), storageId);
    }

    /**
     * Returns all networks of the selected cluster.
     *
     * @return
     */
    @Override
    @GET
    @Path("/networks")
    @Produces("application/json")
    public Response getNetworks() {
        String json = String.format("Provider %d cluster %d networks.", provider.getProviderId(), cluster.getClusterId());
        return Response.ok(json).build();
    }

    /**
     * Creates a new network for the selected cluster.
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
        return new NetworkResource(provider.getProviderId(), cluster.getClusterId(), networkId);
    }

}

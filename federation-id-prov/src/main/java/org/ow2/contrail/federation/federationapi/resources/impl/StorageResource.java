package org.ow2.contrail.federation.federationapi.resources.impl;

import org.ow2.contrail.federation.federationapi.resources.IStorageResource;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

public class StorageResource implements IStorageResource {
    private int providerId;
    private int voId;
    private int edcId;
    private int storageId;

    public StorageResource(int providerId, int voId, int edcId, int storageId) {
        this.providerId = providerId;
        this.voId = voId;
        this.edcId = edcId;
        this.storageId = storageId;
    }

    public StorageResource(int providerId, int storageId) {
        this.providerId = providerId;
        this.storageId = storageId;
    }

    public StorageResource(int providerId, int dcId, int storageId) {
        this.providerId = providerId;
        this.storageId = storageId;
    }

    /**
     * Returns the JSON representation of the given storage.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response getStorage() {
        String json = String.format("Provider %d VO %d EDC %d storage %d",
                providerId, voId, edcId, storageId);
        return Response.ok(json).build();
    }

    /**
     * Updates the given storage.
     *
     * @return
     */
    @PUT
    public Response updateStorage() {
        return Response.ok().build();
    }
}

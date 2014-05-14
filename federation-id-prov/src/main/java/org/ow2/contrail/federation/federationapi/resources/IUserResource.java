package org.ow2.contrail.federation.federationapi.resources;

import org.ow2.contrail.federation.federationapi.interfaces.BaseSingle;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface IUserResource extends BaseSingle {
    /////////////// Attributes

    /**
     * Gets the list of all user’s attributes.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/attributes")
    public Response getAttributes() throws Exception;

    /**
     * Creates a new attribute for a user.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/attributes")
    public Response postAttribute(String content) throws Exception;

    /**
     * Gets the user’s attribute.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/attributes/{attrId}")
    public Response getAttribute(@PathParam("attrId") int attrId) throws Exception;

    /**
     * Modifies or assign a new attribute to a user.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/attributes/{attrId}")
    public Response putAttribute(@PathParam("attrId") int attrId, String content) throws Exception;

    /**
     * Deletes the attribute from a user.
     *
     * @return
     * @throws Exception
     */
    @DELETE
    @Produces("application/json")
    @Path("/attributes/{attrId}")
    public Response deleteAttribute(@PathParam("attrId") int attrId) throws Exception;

    ////////////// roles

    /**
     * Return the list of user identities.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/roles")
    public Response getRoles() throws Exception;

    /**
     * Creates a new user identity for specific identity provider.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/roles")
    public Response postRole(String content) throws Exception;


    /**
     * Return the details about specific user identity.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/roles/{roleId}")
    public Response getRole(@PathParam("roleId") int roleId) throws Exception;

    /**
     * Deletes a user role.
     *
     * @return
     */
    @DELETE
    @Path("/roles/{roleId}")
    public Response deleteRole(@PathParam("roleId") int roleId) throws Exception;

    /////////////////////// groups

    /**
     * Return the list of user identities.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/groups")
    public Response getGroups() throws Exception;

    /**
     * Creates a new user identity for specific identity provider.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/groups")
    public Response postGroups(String content) throws Exception;


    /**
     * Return the details about specific user identity.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/groups/{gid}")
    public Response getGroup(@PathParam("gid") int gId) throws Exception;

    /**
     * Deletes a user role.
     *
     * @return
     */
    @DELETE
    @Path("/groups/{groupId}")
    public Response deleteGroup(@PathParam("groupId") int groupId) throws Exception;

    ///// User Ids

    /**
     * Return the list of user identities.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/ids")
    public Response getIds() throws Exception;

    /**
     * Creates a new user identity for specific identity provider.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/ids")
    public Response postId(String content) throws Exception;

    /**
     * Return the details about specific user identity.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/ids/{idpId}")
    public Response getId(@PathParam("idpId") int idpId) throws Exception;

    /**
     * Creates a new or modifies existing user identity for specific identity provider.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/ids/{idpId}")
    public Response putId(@PathParam("idpId") int idpId, String content) throws Exception;

    /**
     * Deletes the attribute from a user.
     *
     * @return
     * @throws Exception
     */
    @DELETE
    @Produces("application/json")
    @Path("/ids/{idpId}")
    public Response deleteId(@PathParam("idpId") int idpId) throws Exception;

    ///////////////// VOs

    /**
     * Return the list of user identities.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/vos")
    public Response getVOs() throws Exception;

    /**
     * Creates a new user identity for specific identity provider.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/vos")
    public Response postVOs(String content) throws Exception;


    /**
     * Return the details about specific user identity.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/vos/{VOID}")
    public Response getVO(@PathParam("VOID") String VOID) throws Exception;

    /**
     * Creates a new or modifies existing user identity for specific identity provider.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/vos/{VOID}")
    public Response putVO(@PathParam("VOID") String VOID) throws Exception;

    /**
     * Deletes the attribute from a user.
     *
     * @return
     * @throws Exception
     */
    @DELETE
    @Produces("application/json")
    @Path("/vos/{VOID}")
    public Response deleteVO(@PathParam("VOID") String VOID) throws Exception;

    ///////////////// CEEs

    /**
     * Returns the list of constrained execution environments of the user resources.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/cees")
    public Response getCEEs() throws Exception;

    /**
     * Creates a new CEE from the request body description. The URL of the new CEE is returned.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/cees")
    public Response postCEEs(String content) throws Exception;


    /**
     * Return a specific constrained execution environments of the user resources. Re
     * turns the description of CEE CEEID.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/cees/{CEEID}")
    public Response getCEE(@PathParam("CEEID") String CEEID) throws Exception;

    /**
     * Updates the description of CEE CEEID from the message body if this CEE al
     * ready exists. Creates a new CEE with CEEID if it does not exist.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/cees/{CEEID}")
    public Response putCEE(@PathParam("CEEID") String CEEID) throws Exception;

    /**
     * Deletes an existing CEE resource CEEID.
     *
     * @return
     * @throws Exception
     */
    @DELETE
    @Produces("application/json")
    @Path("/cees/{CEEID}")
    public Response deleteCEE(@PathParam("CEEID") String CEEID) throws Exception;

    ///////////////// SLAs

    /**
     * Return the list of service level agreements of the user resources.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/slas")
    public Response getSLAs() throws Exception;

    /**
     * Links a user to an existing SLA.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/slas")
    public Response postSLAs(String content) throws Exception;


    /**
     * Return a specific SLA of the user resources.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/slas/{SLAID}")
    public Response getSLA(@PathParam("SLAID") int SLAID) throws Exception;

    /**
     * Links to or updates an existing SLA link resource.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/slas/{SLAID}")
    public Response putSLA(@PathParam("SLAID") int SLAID, String content) throws Exception;

    /**
     * Deletes an existing SLA link.
     *
     * @return
     * @throws Exception
     */
    @DELETE
    @Produces("application/json")
    @Path("/slas/{SLAID}")
    public Response deleteSLA(@PathParam("SLAID") int SLAID) throws Exception;

    ///////////////// SLAtemplates

    /**
     * Return the list of service level agreement templates (SLAtemplates) of the user resources.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/slats")
    public Response getSLAtemplates() throws Exception;

    /**
     * Links a user to an existing SLAtemplates.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/slats")
    public Response postSLAtemplates(String content) throws Exception;


    /**
     * Return a specific SLAtemplate of the user resources.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/slats/{STID}")
    public Response getSLAtemplate(@PathParam("STID") int STID) throws Exception;

    /**
     * Return SLAs corresponding to SLAtemplate of the user resources.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/slats/{STID}/slas")
    public Response getUserSLAsSLAtemplate(@PathParam("STID") int STID) throws Exception;

    /**
     * Links to or updates an existing SLAtemplate link resource.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/slats/{STID}")
    public Response putSLAtemplate(@PathParam("STID") int STID, String content) throws Exception;

    /**
     * Deletes an existing SLAteamplate link.
     *
     * @return
     * @throws Exception
     */
    @DELETE
    @Produces("application/json")
    @Path("/slats/{STID}")
    public Response deleteSLAtemplate(@PathParam("STID") int STID) throws Exception;

    ///////////////// storage

    /**
     * Returns the list of storages of the user.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/storages")
    public Response getStorages() throws Exception;

    /**
     * Creates a new storage system from the request body description. The URL of the
     * new storage is returned.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/storages")
    public Response postStorages(String content) throws Exception;


    /**
     * Return a specific storages VSTID of the user ID.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/storages/{VSTID}")
    public Response getStorage(@PathParam("VSTID") String VSTID) throws Exception;

    /**
     * Updates the description of storage system VSTID from the message body if this
     * storage already exists. Creates storage system VSTID if it does not exist.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/storages/{VSTID}")
    public Response putStorage(@PathParam("VSTID") String VSTID) throws Exception;

    /**
     * Deletes an existing storage VSTID.
     *
     * @return
     * @throws Exception
     */
    @DELETE
    @Produces("application/json")
    @Path("/storages/{VSTID}")
    public Response deleteStorage(@PathParam("VSTID") String VSTID) throws Exception;

    ///////////////// application

    /**
     * Return the list of all applications of the user ID.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/applications")
    public Response getApplications() throws Exception;

    /**
     * Creates a new application from the request body description. The URL of the new
     * application is returned.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/applications")
    public Response postApplications(String content) throws Exception;


    /**
     * Returns the description of application APPID.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/applications/{appId}")
    public Response getApplication(@PathParam("appId") int appId) throws Exception;

    /**
     * Links user with application.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Path("/applications/{appId}")
    public Response putApplication(@PathParam("appId") int appId, String content) throws Exception;

    /**
     * Deletes an existing application APPID.
     *
     * @return
     * @throws Exception
     */
    @DELETE
    @Produces("application/json")
    @Path("/applications/{appId}")
    public Response deleteApplication(@PathParam("appId") int appId) throws Exception;


    ///////////////// UserOvfs

    /**
     * Return the list of all User ovfs of the user ID.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/ovfs")
    public Response getOvfs() throws Exception;

    /**
     * Creates a new ovf from the request body description. The URL of the new
     * application is returned.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/ovfs")
    public Response postOvf(String content) throws Exception;


    /**
     * Returns the description of ovf.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/ovfs/{ovfId}")
    public Response getOvf(@PathParam("ovfId") int ovfId) throws Exception;

    /**
     * Returns the list of ovf's applications.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/ovfs/{ovfId}/applications")
    public Response getOvfsApplications(@PathParam("ovfId") int ovfId) throws Exception;

    /**
     * Links user with application.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    @Path("/ovfs/{ovfId}")
    public Response putOvf(@PathParam("ovfId") int ovfId, String content) throws Exception;

    /**
     * Deletes an existing application APPID.
     *
     * @return
     * @throws Exception
     */
    @DELETE
    @Produces("application/json")
    @Path("/ovfs/{ovfId}")
    public Response deleteOvf(@PathParam("ovfId") int ovfId) throws Exception;

    //// Providers

    /**
     * Returns a list of providers.
     *
     * @return
     */
    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/providers")
    public Response getProviders() throws Exception;

    /**
     * Get the provider.
     *
     * @return
     */
    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/providers/{provId}")
    public Response getProvider(@PathParam("provId") int provId) throws Exception;

    /**
     * Assignes a new provider to a user.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/providers")
    public Response postProvider(String content) throws Exception;

    /**
     * Deletes a link to a provider.
     *
     * @return
     * @throws Exception
     */
    @DELETE
    @Produces("application/json")
    @Path("/providers/{provId}")
    public Response deleteProvider(@PathParam("provId") int provId) throws Exception;


    //// Dashboard

    /**
     * Returns a user's dashboard
     *
     * @return
     */
    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/dashboard")
    public Response getDashboard() throws Exception;

}

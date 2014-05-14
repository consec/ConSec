package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.mindrot.jbcrypt.BCrypt;
import org.ow2.contrail.federation.federationapi.resources.IUserResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.dao.*;
import org.ow2.contrail.federation.federationdb.jpa.entities.*;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserSLA;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserSLATemplate;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;

public class UserResource implements IUserResource {

    protected static Logger logger =
            Logger.getLogger(UserResource.class);

    private int userId;
    private User user;

    public UserResource(int userId) {
        this.userId = userId;
    }

    public UserResource(User user) {
        this.user = user;
    }

    /**
     * @param providerId Provider ID
     * @param pUserId    Provider specific user ID
     */
    public UserResource(int providerId, int pUserId) {
        userId = providerId * 100 + pUserId; // just for test
    }

    public ArrayList<String> getSubresources() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns the JSON representation of the given user.
     *
     * @return
     */
    public Response get() throws Exception {
        JSONObject user = null;
        user = new JSONObject();
        user.put("username", this.user.getUsername());
        user.put("firstName", this.user.getFirstName());
        user.put("lastName", this.user.getLastName());
        user.put("email", this.user.getEmail());
        user.put("password", this.user.getPassword());
        user.put("uuid", this.user.getUuid());
        user.put("attributes", String.format("/users/%d/attributes", this.user.getUserId()));
        user.put("slas", String.format("/users/%d/slas", this.user.getUserId()));
        user.put("slats", String.format("/users/%d/slats", this.user.getUserId()));
        user.put("applications", String.format("/users/%d/applications", this.user.getUserId()));
        user.put("ids", String.format("/users/%d/ids", this.user.getUserId()));
        user.put("roles", String.format("/users/%d/roles", this.user.getUserId()));
        user.put("groups", String.format("/users/%d/groups", this.user.getUserId()));
        user.put("ovfs", String.format("/users/%d/ovfs", this.user.getUserId()));
        user.put("providers", String.format("/users/%d/providers", this.user.getUserId()));
        return Response.ok(user.toString()).build();
    }

    public Response delete() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            user = em.merge(user);
            em.remove(user);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public Response put(String content) throws Exception {

        JSONObject userData = null;
        try {
            userData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        if (userData.has("username"))
            user.setUsername((String) userData.get("username"));
        if (userData.has("firstName"))
            user.setFirstName((String) userData.get("firstName"));
        if (userData.has("attributes"))
            user.setAttributes((String) userData.get("attributes"));
        if (userData.has("lastName"))
            user.setLastName((String) userData.get("lastName"));
        if (userData.has("password")) {
            String plain_text_password = (String) userData.get("password");
            // Hash a password for the first time
            // gensalt's log_rounds parameter determines the complexity
            // the work factor is 2**log_rounds, and the default is 10
            String hashed = BCrypt.hashpw(plain_text_password, BCrypt.gensalt(12));
            user.setPassword(hashed);
        }
        if (userData.has("email"))
            user.setEmail((String) userData.get("email"));
        if (userData.has("uuid"))
            user.setUuid((String) userData.get("uuid"));

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            user = em.merge(user);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public Response getAttributes() throws Exception {
        JSONArray json = new JSONArray();
        for (UserhasAttribute attribute : user.getUserhasAttributeList()) {
            String uri = String.format("/users/%d/attributes/%d", user.getUserId(),
                    attribute.getUserhasAttributePK().getAttributeId());
            Attribute attr = AttributeDAO.findById(attribute.getUserhasAttributePK().getAttributeId());
            JSONObject o = new JSONObject();
            o.put("name", attr.getName());
            o.put("uri", uri);
            json.put(o);
        }
        return Response.ok(json.toString()).build();
    }

    public Response postAttribute(String content) throws Exception {
        logger.debug("Entring add user attribute.");
        JSONObject json = new JSONObject(content);
        UserhasAttribute attribute = new UserhasAttribute();

        if (json.has("attributeId")) {
            int attributeId = FederationDBCommon.getIdFromString(json.getString("attributeId"));
            logger.debug("Setting attributeId:" + attributeId);
            attribute.setUserhasAttributePK(new UserhasAttributePK(user.getUserId(), attributeId));
        }
        else {
            logger.error("Attribue does not have a name attribute.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        if (json.has("value"))
            attribute.setValue((String) json.get("value"));

        attribute.setUser(this.user);

        if (json.has("referenceId")) {
            logger.debug("Setting referenceId");
            int referenceId = FederationDBCommon.getIdFromString(json.getString("referenceId"));
            attribute.setReferenceId(referenceId);
        }

        user.getUserhasAttributeList().add(attribute);
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(attribute);
            user = em.merge(user);
            em.getTransaction().commit();
        }
        finally {
            logger.debug("Exiting add user attribute.");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }

        URI resourceUri = new URI(String.format("/%d", attribute.getUserhasAttributePK().getAttributeId()));
        return Response.created(resourceUri).build();
    }

    /**
     * Get user attribute.
     */
    public Response getAttribute(int attrId) throws Exception {
        UserhasAttribute userhasattribute = UserhasAttributeDAO.findById(user.getUserId(), attrId);
        Attribute attribute = AttributeDAO.findById(userhasattribute.getUserhasAttributePK().getAttributeId());
        if (userhasattribute == null || attribute == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserhasAttributeResource(user, attribute, userhasattribute).get();
        }
    }

    public Response putAttribute(int attrId, String content)
            throws Exception {
        UserhasAttribute userhasattribute = UserhasAttributeDAO.findById(user.getUserId(), attrId);
        Attribute attribute = AttributeDAO.findById(userhasattribute.getUserhasAttributePK().getAttributeId());
        if (userhasattribute == null || attribute == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserhasAttributeResource(user, attribute, userhasattribute).put(content);
        }
    }

    public Response deleteAttribute(int attrID) throws Exception {
        UserhasAttribute userhasattribute = UserhasAttributeDAO.findById(user.getUserId(), attrID);
        Attribute attribute = AttributeDAO.findById(userhasattribute.getUserhasAttributePK().getAttributeId());
        if (userhasattribute == null || attribute == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserhasAttributeResource(user, attribute, userhasattribute).delete();
        }
    }

    @Override
    public Response getIds() throws Exception {
        JSONArray json = new JSONArray();
        for (UserhasidentityProvider idp : user.getUserhasidentityProviderList()) {
            String uri = String.format("/users/%d/ids/%d", user.getUserId(),
                    idp.getUserhasidentityProviderPK().getIdentityProviderId());
            JSONObject o = new JSONObject();
            o.put("identity", idp.getIdentity());
            o.put("uri", uri);
            json.put(o);
        }
        return Response.ok(json.toString()).build();
    }

    @Override
    public Response postId(String content) throws Exception {
        logger.debug("Entering user postId");
        JSONObject idData = null;
        try {
            idData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            UserhasidentityProvider uid = new UserhasidentityProvider();
            if (!idData.has("identityProviderId")) {
                logger.error("identityProviderId not provided");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
            // Get Idp
            IdentityProvider idp = IdentityProviderDAO.findById(
                    FederationDBCommon.getIdFromString(idData.getString("identityProviderId"))
            );
            if (idp == null) {
                logger.error("Identity Provider with id " + idData.getString("identityProviderId") + " not found.");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            UserhasidentityProvider uhasidp = UserhasIdentityProviderDAO.findById(this.user.getUserId(), idp.getIdentityProviderId());
            if (uhasidp != null) {
                logger.debug("User already has this provider registered");
                // resource is already registered
                return Response.status(Response.Status.CONFLICT).build();
            }
            uid.setIdentityProvider(idp);
            uid.setUserhasidentityProviderPK(new UserhasidentityProviderPK());
            uid.getUserhasidentityProviderPK().setUserId(this.user.getUserId());
            uid.getUserhasidentityProviderPK().setIdentityProviderId(idp.getIdentityProviderId());
            if (idData.has("identity")) {
                uid.setIdentity(idData.getString("identity"));
            }
            else {
                //user identity is mandatory
                logger.error("missing identity - it is mandatory.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
            if (idData.has("attributes"))
                uid.setAttributes(idData.getString("attributes"));
            em.getTransaction().begin();

            user.getUserhasidentityProviderList().add(uid);
            idp.getUserhasidentityProviderList().add(uid);

            em.persist(uid);
            idp = em.merge(idp);
            user = em.merge(user);

            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", uid.getUserhasidentityProviderPK().getIdentityProviderId()));
            logger.debug("Exiting post");
            return Response.created(resourceUri).build();
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            return Response.serverError().build();
        }
        finally {
            logger.debug("Exiting user Ids");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }


    public Response getId(int idpId) throws Exception {
        UserhasidentityProvider uid = UserhasIdentityProviderDAO.findById(this.user.getUserId(), idpId);
        if (uid == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserIdentityProviderResource(this.user, IdentityProviderDAO.findById(idpId), uid).get();
        }
    }

    public Response putId(int idpId, String content) throws Exception {
        UserhasidentityProvider uid = UserhasIdentityProviderDAO.findById(this.user.getUserId(), idpId);
        if (uid == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserIdentityProviderResource(this.user, IdentityProviderDAO.findById(idpId), uid).put(content);
        }
    }

    public Response deleteId(int idpId) throws Exception {
        UserhasidentityProvider uid = UserhasIdentityProviderDAO.findById(this.user.getUserId(), idpId);
        if (uid == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserIdentityProviderResource(this.user, IdentityProviderDAO.findById(idpId), uid).delete();
        }
    }

    /////// VOs

    public Response getVOs() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response postVOs(String content) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response getVO(String VOID) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response putVO(String VOID) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response deleteVO(String VOID) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response getCEEs() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response postCEEs(String content) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response getCEE(String CEEID) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response putCEE(String CEEID) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response deleteCEE(String CEEID) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Get user SLAs.
     */
    public Response getSLAs() throws Exception {
        logger.debug("Entering get SLAs");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONArray UriList = new JSONArray();
            for (UserSLA sla : this.user.getUserSLAList()) {
                String uri = String.format("/users/%d/slas/%d", user.getUserId(), sla.getSLAId());
                JSONObject o = new JSONObject();
                o.put("name", sla.getSLAId());
                o.put("uri", uri);
                UriList.put(o);
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            logger.debug("Exiting get SLAs");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public Response postSLAs(String content) throws Exception {
        logger.debug("Entering user post SLAs");
        JSONObject slaData = null;
        try {
            slaData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            UserSLA sla = new UserSLA();
            if (slaData.has("sla"))
                sla.setSla(slaData.getString("sla"));
            if (slaData.has("content"))
                sla.setContent(slaData.getString("content"));
            if (slaData.has("templateurl"))
                sla.setTemplateURL(slaData.getString("templateurl"));
            if (slaData.has("slatId")) {
                org.ow2.contrail.federation.federationdb.jpa.entities.UserSLATemplate slat =
                        UserSLATemplateDAO.findById(FederationDBCommon.getIdFromString(slaData.getString("slatId")));
                if (slat != null) {
                    logger.debug("Found UserSLATemplate with id " + slat.getSlatId());
                    sla.setSLATemplateId(slat);
                    slat.getUserSLAList().add(sla);
                    slat = em.merge(slat);
                }
                else {
                    logger.error("Could not found SLATemplate with id: " + slaData.getString("slatId"));
                }
            }
            sla.setUserId(this.user);
            em.getTransaction().begin();
            em.persist(sla);
            user.getUserSLAList().add(sla);
            user = em.merge(user);
            em.getTransaction().commit();
            URI resourceUri = new URI(String.format("/%d", sla.getSLAId()));
            logger.debug("Exiting post");
            return Response.created(resourceUri).build();
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            return Response.serverError().build();
        }
        finally {
            logger.debug("Exiting user post SLAs");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public Response getSLA(int SLAID) throws Exception {
        UserSLA sla = UserSLADAO.findById(SLAID);
        if (sla == null || !user.getUserSLAList().contains(sla)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new org.ow2.contrail.federation.federationapi.resources.impl.UserSLA(user, sla).get();
        }
    }

    public Response putSLA(int SLAID, String content) throws Exception {
        UserSLA sla = UserSLADAO.findById(SLAID);
        if (sla == null || !user.getUserSLAList().contains(sla)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new org.ow2.contrail.federation.federationapi.resources.impl.UserSLA(user, sla).put(content);
        }
    }

    public Response deleteSLA(int SLAID) throws Exception {
        UserSLA sla = UserSLADAO.findById(SLAID);
        if (sla == null || !user.getUserSLAList().contains(sla)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new org.ow2.contrail.federation.federationapi.resources.impl.UserSLA(user, sla).delete();
        }
    }

    public Response getSLAtemplates() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONArray UriList = new JSONArray();
            for (UserSLATemplate slat : this.user.getUserSLATemplateList()) {
                String uri = String.format("/users/%d/slats/%d", user.getUserId(), slat.getSLATemplateId());
                JSONObject o = new JSONObject();
                o.put("name", slat.getSLATemplateId());
                o.put("uri", uri);
                UriList.put(o);
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public Response postSLAtemplates(String content) throws Exception {
        logger.debug("Entering post");
        JSONObject slaTemplateData = null;
        try {
            slaTemplateData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            UserSLATemplate slat = new UserSLATemplate();
            if (slaTemplateData.has("url"))
                slat.setUrl(slaTemplateData.getString("url"));
            if (slaTemplateData.has("content"))
                slat.setContent(slaTemplateData.getString("content"));
            if (slaTemplateData.has("slatId")) {
                SLATemplate resslat = SLATemplateDAO.findById(FederationDBCommon.getIdFromString(slaTemplateData.getString("slatId")));
                if (resslat != null) {
                    logger.debug("Found SLATemplate with id " + resslat.getSlatId());
                    slat.setSlatId(resslat);
                    resslat.getUserSLATemplateList().add(slat);
                    resslat = em.merge(resslat);
                }
                else {
                    logger.error("Could not found SLATemplate with id: " + slaTemplateData.getString("slatId"));
                }
            }
            slat.setUserId(user);
            em.getTransaction().begin();
            em.persist(slat);
            user.getUserSLATemplateList().add(slat);
            user = em.merge(user);
            em.getTransaction().commit();
            URI resourceUri = new URI(String.format("/%d", slat.getSLATemplateId()));
            logger.debug("Exiting post");
            return Response.created(resourceUri).build();
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            return Response.serverError().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public Response getSLAtemplate(int STID) throws Exception {
        UserSLATemplate slat = UserSLATemplateDAO.findById(STID);
        if (slat == null || !user.getUserSLATemplateList().contains(slat)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new org.ow2.contrail.federation.federationapi.resources.impl.UserSLATemplate(user, slat).get();
        }
    }

    public Response getUserSLAsSLAtemplate(int STID) throws Exception {
        UserSLATemplate slat = UserSLATemplateDAO.findById(STID);
        if (slat == null || !user.getUserSLATemplateList().contains(slat)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new org.ow2.contrail.federation.federationapi.resources.impl.UserSLATemplate(user, slat).getSLATsUserSLA();
        }
    }

    public Response putSLAtemplate(int STID, String content) throws Exception {
        UserSLATemplate slat = UserSLATemplateDAO.findById(STID);
        if (slat == null || !user.getUserSLATemplateList().contains(slat)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new org.ow2.contrail.federation.federationapi.resources.impl.UserSLATemplate(user, slat).put(content);
        }
    }

    public Response deleteSLAtemplate(int STID) throws Exception {
        UserSLATemplate slat = UserSLATemplateDAO.findById(STID);
        if (slat == null || !user.getUserSLATemplateList().contains(slat)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new org.ow2.contrail.federation.federationapi.resources.impl.UserSLATemplate(user, slat).delete();
        }
    }

    public Response getStorages() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response postStorages(String content) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response getStorage(String VSTID) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response putStorage(String VSTID) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response deleteStorage(String VSTID) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public Response getApplications() throws Exception {
        JSONArray json = new JSONArray();
        for (Application application : user.getApplicationList()) {
            String uri = String.format("/users/%d/applications/%d", user.getUserId(),
                    application.getApplicationId());
            JSONObject o = new JSONObject();
            o.put("name", application.getName());
            o.put("uri", uri);
            json.put(o);

        }
        return Response.ok(json.toString()).build();
    }

    @Override
    public Response putApplication(int appId, String content) throws Exception {
        logger.debug("Entering put");
        Application application = ApplicationDAO.findById(appId);
        if (application == null || !user.getApplicationList().contains(application)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new ApplicationResource(application, user).put(content);
        }
    }

    /**
     * Add application to a user.
     */
    public Response postApplications(String content) throws Exception {
        logger.debug("Entering user add application.");
        JSONObject appData = null;
        try {
            appData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        String name = (String) appData.get("name");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            for (Application app : user.getApplicationList()) {
                if (app.getName().equals(name)) {
                    return Response.status(Response.Status.CONFLICT).build();
                }
            }

            Application app = new Application();
            if (appData.has("name"))
                app.setName(name);
            else {
                logger.error("The application does not have a name attribute.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

            if (appData.has("deploymentDesc"))
                app.setDeploymentDesc((String) appData.get("deploymentDesc"));
            if (appData.has("applicationOvf"))
                app.setApplicationOvf((String) appData.get("applicationOvf"));
            if (appData.has("attributes"))
                app.setAttributes((String) appData.get("attributes"));

            em.getTransaction().begin();
            em.persist(app);
            user.getApplicationList().add(app);
            user = em.merge(user);
            em.getTransaction().commit();
            URI resourceUri = new URI(String.format("/%d", app.getApplicationId()));
            return Response.created(resourceUri).build();
        }
        finally {
            logger.debug("Exiting user add application.");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Get the user application
     */
    public Response getApplication(int appId) throws Exception {
        Application app = ApplicationDAO.findById(appId);
        if (app == null || !user.getApplicationList().contains(app)) {
            logger.debug("User does not have application with id:" + appId);
            logger.debug("Application with " + appId + " does not exist.");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new ApplicationResource(app, user).get();
        }
    }

    /**
     * Get the user application
     */
    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/applications/{appId}/ovfs")
    public Response getUserApplicationsOvfs(@PathParam("appId") int appId) throws Exception {
        JSONArray json = new JSONArray();
        Application app = ApplicationDAO.findById(appId);
        for (UserOvf ovf : app.getUserOvfList()) {
            String uri = String.format("/users/%d/applications/%d/ovfs/%d", user.getUserId(), appId, ovf.getOvfId());
            JSONObject o = new JSONObject();
            o.put("name", ovf.getName());
            o.put("uri", uri);
            json.put(o);
        }
        return Response.ok(json.toString()).build();
    }

    /**
     * Create an Ovf inside user application.
     */
    @POST
    @Consumes("application/json")
    @Path("/applications/{appId}/ovfs")
    public Response postUserApplicationsOvfs(@PathParam("appId") int appId, String content) throws Exception {
        JSONObject data = null;
        Application app = ApplicationDAO.findById(appId);
        if (app == null) {
            logger.error("not found");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        try {
            data = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        if (!data.has("ovfId")) {
            logger.error("User groups ID has to be provided.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            String strovfId = data.getString("ovfId");
            // Get Ovf
            UserOvf ovf = UserOvfDAO.findById(
                    FederationDBCommon.getIdFromString(strovfId)
            );
            if (ovf == null) {
                logger.error("Identity Provider with id " + data.getString("ovfId") + " not found.");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            em.getTransaction().begin();
            app.getUserOvfList().add(ovf);
            ovf.getApplicationList().add(app);
            ovf = em.merge(ovf);
            app = em.merge(app);
            em.getTransaction().commit();
            URI resourceUri = new URI(String.format("/users/%d/applications/%d/ovfs/%d", user.getUserId(), app.getApplicationId(), ovf.getOvfId()));
            return Response.created(resourceUri).build();
        }
        finally {
            logger.debug("Exiting post application ovf.");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Get the user application
     */
    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/applications/{appId}/ovfs/{ovfId}")
    public Response getUserApplicationsOvf(@PathParam("appId") int appId, @PathParam("ovfId") int ovfId) throws Exception {
        logger.debug("Entering delete.");
        UserOvf ovf = UserOvfDAO.findById(ovfId);
        Application app = ApplicationDAO.findById(appId);
        if (ovf == null || !ovf.getApplicationList().contains(app)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserOvfResource(user, app, ovf).get();
        }
    }

    /**
     * Get the user application
     */
    @DELETE
    @Path("/applications/{appId}/ovfs/{ovfId}")
    public Response deleteUserApplicationsOvf(@PathParam("appId") int appId, @PathParam("ovfId") int ovfId) throws Exception {
        logger.debug("Entering delete");
        UserOvf ovf = UserOvfDAO.findById(ovfId);
        Application app = ApplicationDAO.findById(appId);
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            app.getUserOvfList().remove(ovf);
            ovf.getApplicationList().remove(app);
            app = em.merge(app);
            ovf = em.merge(ovf);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Exiting delete");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Delete the user application
     */
    public Response deleteApplication(int appId) throws Exception {
        logger.debug("Entering delete.");
        Application application = ApplicationDAO.findById(appId);
        if (application == null || !user.getApplicationList().contains(application)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new ApplicationResource(application, user).delete();
        }
    }

    @Override
    public Response getRoles() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONArray UriList = new JSONArray();
            for (URole role : this.user.getURoleList()) {
                String uri = String.format("/roles/%d", role.getRoleId());
                JSONObject o = new JSONObject();
                o.put("name", role.getName());
                o.put("uri", uri);
                UriList.put(o);
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @Override
    public Response postRole(String content) throws Exception {
        JSONObject roleData = null;
        try {
            roleData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        if (!roleData.has("roleId")) {
            logger.error("User role ID has to be provided.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            String roleString = roleData.getString("roleId");
            int roleId = Integer.parseInt(roleString.substring(roleString.lastIndexOf("/") + 1));
            logger.debug("Got Role Id: " + roleId);
            URole newrole = URoleDAO.findById(roleId);
            user.getURoleList().add(newrole);
            newrole.getUserList().add(user);
            newrole = em.merge(newrole);
            em.getTransaction().begin();
            user = em.merge(user);
            em.flush();
            em.getTransaction().commit();
            URI resourceUri = new URI(String.format("/%d", newrole.getRoleId()));
            return Response.created(resourceUri).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @Override
    public Response getRole(int roleId) throws Exception {
        URole role = URoleDAO.findById(roleId);
        if (role == null || !role.getUserList().contains(user)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new URoleResource(user, role).get();
        }
    }

    @Override
    public Response deleteRole(int roleId) throws Exception {
        URole role = URoleDAO.findById(roleId);
        if (role == null || !role.getUserList().contains(user)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            logger.debug(String.format("Deleting role %d for user %d", role.getRoleId(), user.getUserId()));
            return new URoleResource(user, role).deleteUserRole();
        }
    }

    @Override
    public Response getGroups() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONArray UriList = new JSONArray();
            for (UGroup group : this.user.getUGroupList()) {
                String uri = String.format("/groups/%d", group.getGroupId());
                JSONObject o = new JSONObject();
                o.put("name", group.getName());
                o.put("uri", uri);
                UriList.put(o);
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @Override
    public Response postGroups(String content) throws Exception {
        JSONObject groupData = null;
        try {
            groupData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        if (!groupData.has("groupId")) {
            logger.error("User groups ID has to be provided.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            String groupString = groupData.getString("groupId");
            int groupId = Integer.parseInt(groupString.substring(groupString.lastIndexOf("/") + 1));
            logger.debug("Got Group Id: " + groupId);
            UGroup newgroup = UGroupDAO.findById(groupId);
            user.getUGroupList().add(newgroup);
            newgroup.getUserList().add(user);
            newgroup = em.merge(newgroup);
            em.getTransaction().begin();
            user = em.merge(user);
            em.getTransaction().commit();
            URI resourceUri = new URI(String.format("/%d", newgroup.getGroupId()));
            return Response.created(resourceUri).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @Override
    public Response getGroup(int gId) throws Exception {
        UGroup group = UGroupDAO.findById(gId);
        if (group == null || !group.getUserList().contains(user)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UGroupResource(user, group).get();
        }
    }

    @Override
    public Response deleteGroup(int groupId) throws Exception {
        UGroup group = UGroupDAO.findById(groupId);
        if (group == null || !group.getUserList().contains(user)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UGroupResource(user, group).deleteUserGroup();
        }
    }

    /// UserOVFs
    @Override
    public Response getOvfs() throws Exception {
        JSONArray json = new JSONArray();
        for (UserOvf ovf : user.getUserOvfList()) {
            String uri = String.format("/users/%d/ovfs/%d", user.getUserId(), ovf.getOvfId());
            JSONObject o = new JSONObject();
            o.put("name", ovf.getName());
            o.put("uri", uri);
            json.put(o);
        }
        return Response.ok(json.toString()).build();
    }

    @Override
    public Response postOvf(String content) throws Exception {
        logger.debug("Entering post of users OVFs");
        JSONObject ovfjson = null;
        try {
            ovfjson = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            UserOvf userovf = new UserOvf();
            if (ovfjson.has("name"))
                userovf.setName(ovfjson.getString("name"));
            if (ovfjson.has("attributes"))
                userovf.setAttributes(ovfjson.getString("attributes"));
            if (ovfjson.has("content"))
                userovf.setContent(ovfjson.getString("content"));
            if (ovfjson.has("providerOvfId")) {
                int providerOvfId = Integer.parseInt(ovfjson.getString("providerOvfId").substring(ovfjson.getString("providerOvfId").lastIndexOf("/") + 1));
                logger.debug("Got providerOvfId " + providerOvfId);
                Ovf providerOvf = OvfDAO.findById(providerOvfId);
                if (providerOvf == null) {
                    logger.error("providerOvfId attribute missing");
                    return Response.status(Response.Status.NOT_ACCEPTABLE).build();
                }
                userovf.setProviderOvfId(providerOvf);
            }
            else {
                logger.error("providerOvfId attribute missing");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
            userovf.setUserId(this.user);
            em.getTransaction().begin();
            em.persist(userovf);
            user.getUserOvfList().add(userovf);
            user = em.merge(user);
            em.getTransaction().commit();
            URI resourceUri = new URI(String.format("/%d", userovf.getOvfId()));
            logger.debug("Exiting post of users application's OVF");
            return Response.created(resourceUri).build();
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            return Response.serverError().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @Override
    public Response getOvf(int ovfId) throws Exception {
        UserOvf ovf = UserOvfDAO.findById(ovfId);
        if (ovf == null || !user.getUserOvfList().contains(ovf)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserOvfResource(user, ovf).get();
        }
    }

    @Override
    public Response getOvfsApplications(int ovfId) throws Exception {
        UserOvf ovf = UserOvfDAO.findById(ovfId);
        if (ovf == null || !user.getUserOvfList().contains(ovf)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserOvfResource(user, ovf).getApplications();
        }
    }

    @Override
    public Response putOvf(int ovfId, String content) throws Exception {
        UserOvf ovf = UserOvfDAO.findById(ovfId);
        if (ovf == null || !user.getUserOvfList().contains(ovf)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserOvfResource(user, ovf).put(content);
        }
    }

    @Override
    public Response deleteOvf(int ovfId) throws Exception {
        UserOvf ovf = UserOvfDAO.findById(ovfId);
        if (ovf == null || !user.getUserOvfList().contains(ovf)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            logger.debug(String.format("Deleting ovf %d for user %d", ovf.getOvfId(), user.getUserId()));
            return new UserOvfResource(user, ovf).delete();
        }
    }

    @Override
    public Response postProvider(String content) throws Exception {
        JSONObject provData = null;
        try {
            provData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        if (!provData.has("providerId")) {
            logger.error("provider ID has to be provided.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            String provString = provData.getString("providerId");
            // Get Idp
            Provider idp = ProviderDAO.findById(
                    FederationDBCommon.getIdFromString(provString)
            );
            if (idp == null) {
                logger.error("Identity Provider with id " + provData.getString("identityProvierId") + " not found.");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            em.getTransaction().begin();
            idp.getUserList().add(this.user);
            this.user.getProviderList().add(idp);
            user = em.merge(user);
            idp = em.merge(idp);
            em.getTransaction().commit();
            URI resourceUri = new URI(String.format("/users/%d/providers/%d", this.user.getUserId(), idp.getProviderId()));
            return Response.created(resourceUri).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @Override
    public Response deleteProvider(int provId) throws Exception {
        Provider prov = ProviderDAO.findById(provId);
        if (prov == null || !user.getProviderList().contains(prov)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            EntityManager em = PersistenceUtils.getInstance().getEntityManager();
            em.getTransaction().begin();
            this.user.getProviderList().remove(prov);
            prov.getUserList().remove(this.user);
            this.user = em.merge(this.user);
            prov = em.merge(prov);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }

    /**
     * Return what getProvider returns.
     */
    @Override
    public Response getProvider(int provId) throws Exception {
        Provider prov = ProviderDAO.findById(provId);
        if (prov == null || !user.getProviderList().contains(prov)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new ProviderResource(prov).getProvider();
        }
    }

    /**
     * Build a list of providers with their names.
     */
    @Override
    public Response getProviders() throws Exception {
        JSONArray json = new JSONArray();
        JSONObject o = new JSONObject();
        for (Provider provider : user.getProviderList()) {
            String name = provider.getName();
            String uri = String.format("/providers/%d", provider.getProviderId());
            o.put("name", name);
            o.put("uri", uri);
            json.put(o);
        }
        return Response.ok(json.toString()).build();
    }

    @Override
    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/dashboard")
    public Response getDashboard() throws Exception {
        logger.debug("Entering GET dashboard");
        JSONObject jsonDashboard = null;
        jsonDashboard = new JSONObject();
        JSONArray jsonTempArray = new JSONArray();
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            jsonTempArray = new JSONArray();
            for (URole role : this.user.getURoleList()) {
                String uri = String.format("/roles/%d", role.getRoleId());
                JSONObject o = new JSONObject();
                o.put("name", role.getName());
                o.put("uri", uri);
                jsonTempArray.put(o);
            }
            jsonDashboard.put("roles", jsonTempArray);

            jsonTempArray = new JSONArray();
            for (UserhasAttribute attribute : user.getUserhasAttributeList()) {
                String uri = String.format("/users/%d/attributes/%d", user.getUserId(),
                        attribute.getUserhasAttributePK().getAttributeId());
                Attribute attr = AttributeDAO.findById(attribute.getUserhasAttributePK().getAttributeId());
                JSONObject o = new JSONObject();
                o.put("name", attr.getName());
                o.put("uri", uri);
                jsonTempArray.put(o);
            }
            jsonDashboard.put("attributes", jsonTempArray);

            jsonTempArray = new JSONArray();
            for (UGroup group : this.user.getUGroupList()) {
                String uri = String.format("/groups/%d", group.getGroupId());
                JSONObject o = new JSONObject();
                o.put("name", group.getName());
                o.put("uri", uri);
                jsonTempArray.put(o);
            }
            jsonDashboard.put("groups", jsonTempArray);
            jsonTempArray = new JSONArray();
            for (UserhasidentityProvider idp : user.getUserhasidentityProviderList()) {
                String uri = String.format("/users/%d/ids/%d", user.getUserId(),
                        idp.getUserhasidentityProviderPK().getIdentityProviderId());
                JSONObject o = new JSONObject();
                o.put("identity", idp.getIdentity());
                o.put("uri", uri);
                jsonTempArray.put(o);
            }
            jsonDashboard.put("identities", jsonTempArray);
            jsonTempArray = new JSONArray();
            for (Application application : user.getApplicationList()) {
                String uri = String.format("/users/%d/applications/%d", user.getUserId(),
                        application.getApplicationId());
                JSONObject o = new JSONObject();
                o.put("name", application.getName());
                o.put("state", application.getState());
                o.put("slaUri", application.getSlaUrl());
                o.put("applicationOvf", application.getApplicationOvf());
                o.put("deploymentDesc", application.getDeploymentDesc());
                o.put("uri", uri);
                jsonTempArray.put(o);
            }
            jsonDashboard.put("applications", jsonTempArray);
        }
        catch (Exception err) {
            logger.error("Error in dashboard:");
            logger.error(FederationDBCommon.getStackTrace(err));
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }

        logger.debug("Exiting GET dashboard");
        return Response.ok(jsonDashboard.toString()).build();
    }

}

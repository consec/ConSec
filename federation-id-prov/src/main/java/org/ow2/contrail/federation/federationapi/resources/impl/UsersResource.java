/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.ow2.contrail.federation.federationapi.resources.IUserResource;
import org.ow2.contrail.federation.federationapi.resources.IUsersResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationdb.jpa.dao.AttributeDAO;
import org.ow2.contrail.federation.federationdb.jpa.dao.UserDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.Attribute;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserhasAttribute;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserhasAttributePK;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * @author ales
 */
@Path("/users")
public class UsersResource implements IUsersResource {

    protected static Logger logger =
            Logger.getLogger(UsersResource.class);

    @Context
    HttpServletRequest request;

    public Response get() throws Exception {
        logger.debug("Entering get");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createNamedQuery("User.findAll");
            List<User> userList = query.getResultList();
            JSONArray UriList = new JSONArray();
            for (User user : userList) {
                String uri = String.format("/users/%d", user.getUserId());
                JSONObject o = new JSONObject();
                o.put("username", user.getUsername());
                o.put("uri", uri);
                UriList.put(o);
            }
            logger.debug("Exiting get");
            return Response.ok(UriList.toString()).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public Response post(String content) throws Exception {
        logger.debug("Entering post");
        JSONObject userData = null;
        try {
            userData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        String name = (String) userData.get("username");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username=:name");
            query.setParameter("name", name);
            if ((Long) query.getSingleResult() > 0) {
                // resource is already registered
                return Response.status(Response.Status.CONFLICT).build();
            }

            if (userData.has("email")) {
                String existingEmail = (String) userData.get("email");
                User existingUser = null;
                try {
                    existingUser = UserDAO.findByEmail(existingEmail);
                    if (existingUser != null) {
                        logger.error("User with email " + existingEmail + " already exists: " + existingUser.getUsername());
                        // resource is already registered
                        return Response.status(Response.Status.CONFLICT).build();
                    }
                }
                catch (Exception e) {
                    logger.debug("No users fund with this email. This is OK.");
                }
            }

            User user = new User();
            user.setUsername(name);
            if (userData.has("firstName"))
                user.setFirstName((String) userData.get("firstName"));
            if (userData.has("attributes"))
                user.setAttributes((String) userData.get("attributes"));
            if (userData.has("lastName"))
                user.setLastName((String) userData.get("lastName"));

            String plain_text_password = (String) userData.get("password");
            // Hash a password for the first time
            // gensalt's log_rounds parameter determines the complexity
            // the work factor is 2**log_rounds, and the default is 10
            String hashed = BCrypt.hashpw(plain_text_password, BCrypt.gensalt(12));
            user.setPassword(hashed);

            user.setEmail((String) userData.get("email"));
            user.setUuid(UUID.randomUUID().toString());

            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", user.getUserId()));
            // create user slasoi id:
            logger.debug("Adding user slasoi attribute");
            try {
                Attribute attrSlaSoi = AttributeDAO.findByName(AttributeResource.PROVIDER_SUBJECT_SLASOI_ID);
                UserhasAttribute attribute = new UserhasAttribute();
                attribute.setUser(user);
                attribute.setReferenceId(user.getUserId());
                attribute.setValue((user.getUserId().toString()));
                attribute.setUserhasAttributePK(new UserhasAttributePK(user.getUserId(), attrSlaSoi.getAttributeId()));
                user.getUserhasAttributeList().add(attribute);
                em.getTransaction().begin();
                em.persist(attribute);
                user = em.merge(user);
                em.getTransaction().commit();
            }
            catch (javax.persistence.NoResultException err) {
                logger.debug("This happens when slasoi attribute is missing in the federation DB: ");
                logger.debug(FederationDBCommon.getStackTrace(err));
                logger.debug("Skipping slasoi attribute addition since the attribute is missing in the database.");
            }

            logger.debug("Exiting post");
            return Response.created(resourceUri).build();
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.serverError().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public IUserResource subResource(int id) throws Exception {
        User user = UserDAO.findById(id);
        if (user == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserResource(user);
        }
    }

    /**
     * Authentication of users against the database. Consumes JSON document:
     * POST /users/authenticate {username, password}
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Path("/authenticate")
    public Response authenticate(String content) throws Exception {
        logger.debug("Entering post/authenticate");
        JSONObject userData = null;
        try {
            userData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        if (!userData.has("username") || !userData.has("password")) {
            logger.debug("Username or password missing in the JSON document.");
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            User user = UserDAO.findByUsername((String) userData.get("username"));
            if (user == null) {
                // User does not exist
                logger.debug("User not found. Resonse is UNAUTHORIZED");
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            // Check that an unencrypted password matches one that has
            // previously been hashed
            if (BCrypt.checkpw((String) userData.get("password"), user.getPassword())) {
                logger.debug("Found user and password maches!");
            }
            else {
                logger.debug("Found the user but passwords do no match. ");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

            // Return user data
            String uri = String.format("/users/%d", user.getUserId());
            JSONObject o = new JSONObject();
            o.put("username", user.getUsername());
            o.put("uri", uri);
            logger.debug("Exiting get with return data: " + o.toString());
            return Response.ok(o.toString()).build();
            //URI resourceUri = new URI(String.format("/%d",user.getUserId()));
            //logger.debug("Exiting post");
            //return Response.ok().build();
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            return Response.serverError().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

}

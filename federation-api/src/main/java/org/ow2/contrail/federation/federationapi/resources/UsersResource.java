/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.dao.AttributeDAO;
import org.consec.federationdb.dao.UserDAO;
import org.consec.federationdb.model.Attribute;
import org.consec.federationdb.model.User;
import org.consec.federationdb.model.UserHasAttribute;
import org.consec.federationdb.model.UserHasAttributePK;
import org.consec.federationdb.utils.EMF;
import org.json.JSONArray;
import org.mindrot.jbcrypt.BCrypt;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationapi.utils.RestUriBuilder;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * @author ales
 */
@Path("/users")
public class UsersResource {

    protected static Logger logger = Logger.getLogger(UsersResource.class);

    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        logger.debug("Entering get");

        EntityManager em = EMF.createEntityManager();
        try {
            Query query = em.createNamedQuery("User.findAll");
            List<User> userList = query.getResultList();
            JSONArray uriList = new JSONArray();
            for (User user : userList) {
                JSONObject o = new JSONObject();
                o.put("username", user.getUsername());
                o.put("uri", RestUriBuilder.getUserUri(user));
                uriList.put(o);
            }
            logger.debug("Exiting get");
            return Response.ok(uriList.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    /**
     * Creates a new user in the federation api database. Also, forwards
     * a user creation on the federation identity provider is provided.
     *
     * @param userData JSONObject
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response post(JSONObject userData) throws Exception {
        logger.debug("Entering post");

        EntityManager em = EMF.createEntityManager();

        try {
            Query query = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username=:name");
            query.setParameter("name", userData.getString("username"));
            if ((Long) query.getSingleResult() > 0) {
                // resource is already registered
                return Response.status(Response.Status.CONFLICT).build();
            }

            if (userData.has("email")) {
                String existingEmail = (String) userData.get("email");
                User existingUser = null;
                try {
                    existingUser = new UserDAO(em).findByEmail(existingEmail);
                    if (existingUser != null) {
                        logger.error("User with email " + existingEmail + " already exists: " + existingUser.getUsername());
                        // resource is already registered
                        return Response.status(Response.Status.CONFLICT).build();
                    }
                }
                catch (Exception e) {
                    logger.debug("No users found with this email. This is OK.");
                }
            }

            User user = new User();
            user.setUsername(userData.getString("username"));
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
            user.setUserId(UUID.randomUUID().toString());

            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();

            URI resourceUri = new URI(user.getUserId());
            // create user slasoi id:
            logger.debug("Adding user slasoi attribute");
            try {
                Attribute attrSlaSoi = new AttributeDAO(em).findByName(AttributeResource.PROVIDER_SUBJECT_SLASOI_ID);
                UserHasAttribute userAttr = new UserHasAttribute();
                userAttr.setUser(user);
                //userAttr.setReferenceId(user.getUserId()); ??????????
                userAttr.setValue((user.getUserId()));
                userAttr.setUserHasAttributePK(
                        new UserHasAttributePK(user.getUserId(), attrSlaSoi.getAttributeId()));
                user.getUserHasAttributeList().add(userAttr);
                em.getTransaction().begin();
                em.persist(userAttr);
                em.getTransaction().commit();
            }
            catch (javax.persistence.NoResultException err) {
                logger.debug("This happens when slasoi attribute is missing in the federation DB: ");
                logger.debug(FederationDBCommon.getStackTrace(err));
                logger.debug("Skipping slasoi attribute addition since the attribute is missing in the database.");
            }

            try {
                JSONObject idProvUserData = new JSONObject(userData.toString());
                idProvUserData.put("uuid", user.getUserId());
            }
            catch (Exception err) {
                logger.error(FederationDBCommon.getStackTrace(err));
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
            EMF.closeEntityManager(em);
        }
    }
}

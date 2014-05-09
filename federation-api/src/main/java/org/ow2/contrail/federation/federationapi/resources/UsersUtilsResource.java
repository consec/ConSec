package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.dao.AttributeDAO;
import org.consec.federationdb.dao.UserDAO;
import org.consec.federationdb.dao.UserHasIdentityProviderDAO;
import org.consec.federationdb.model.Attribute;
import org.consec.federationdb.model.*;
import org.consec.federationdb.utils.EMF;
import org.joda.time.DateTime;
import org.mindrot.jbcrypt.BCrypt;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.AttributeQueryImpl;
import org.opensaml.ws.soap.soap11.Body;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.xml.io.MarshallingException;
import org.ow2.contrail.federation.federationapi.MyServletContextListener;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationapi.utils.RestUriBuilder;
import org.ow2.contrail.federation.federationapi.utils.saml.PrettyPrinter;
import org.ow2.contrail.federation.federationapi.utils.saml.SAML;
import org.w3c.dom.Document;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.transform.TransformerException;
import java.util.*;

@Path("/usersutils")
public class UsersUtilsResource {
	protected static Logger logger = Logger.getLogger(UsersResource.class);

	/**
	 * Method filters user from filter given in the JSON body.
	 * <p/>
	 * Format: { "user:email":"coordinator@contrail.eu" } returns
	 * {"username":"coordinator"
	 * ,"uuid":"5a947f8c-83d3-4da0-a52c-d9436ae77bb5","uri":"/users/1"}
	 * <p/>
	 * but { "user:email":"coordinator@il.eu" } reutrns 204 (No Content).
	 * 
	 * @param userData
	 * @return 
	 *         {"username":"coordinator","uuid":"5a947f8c-83d3-4da0-a52c-d9436ae77bb5"
	 *         ,"uri":"/users/1"} or HTTP 204 if no user is found.
	 * @throws Exception
	 */
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	@Path("/filter")
	public Response filter(JSONObject userData) throws Exception {
		logger.debug("Entering POST /filter");

		EntityManager em = EMF.createEntityManager();
		try {
			Iterator<String> userDataIterator = userData.keys();
			String strKey = userDataIterator.next();
			User foundUser = null;
			while (strKey != null) {
				logger.debug("Entering mark1");
				String value = userData.getString(strKey);
				logger.debug("value = " + strKey);
				try {
					SearchFilter filterBase = null;
					for (SearchFilter filter : SearchFilter.values()) {
						if (filter.value().equals(strKey)) {
							filterBase = filter;
							break;
						}
					}
					if (filterBase == null)
						throw new Exception(String.format(
								"Filter %s not known.", strKey));
					switch (filterBase) {
					case USER_EMAIL:
						logger.debug("Entering USER_MAIL");
						foundUser = new UserDAO(em).findByEmail(value);
						break;
					case USER_IDP_IDENTITY:
						logger.debug("Entering USER_IDP_IDENTITY");
                        UserHasIdentityProvider userIdp = new UserHasIdentityProviderDAO(em).findByIdentity(value);
						if (userIdp == null) {
							logger.error(String.format(
									"User with identity %s not found.", value));
							return Response.status(Response.Status.NOT_FOUND)
									.build();
						}
						foundUser = userIdp.getUser();
						logger.debug(String.format(
								"User with identity %s found: %s", value,
								foundUser.getUserId()));
						break;
					case USER_USERNAME:
						logger.debug("Entering USER_USERNAME");
						foundUser = new UserDAO(em).findByUsername(value);
						break;
					default:
                        return Response.status(Response.Status.BAD_REQUEST).entity("Invalid filter.").build();
					}
				} catch (Exception e) {
					logger.error(e);
					return Response.status(Response.Status.NOT_ACCEPTABLE)
							.build();
				}

				try {
					strKey = userDataIterator.next();
				} catch (NoSuchElementException err) {
					// No more keys
					strKey = null;
				}
			}
			if (foundUser == null) {
				// Return empty response
				return Response.status(Response.Status.NO_CONTENT).build();
			}
			// Return user data
			JSONObject o = new JSONObject();
			o.put("username", foundUser.getUsername());
			o.put("uri", RestUriBuilder.getUserUri(foundUser));
			o.put("uuid", foundUser.getUserId());
			logger.debug("Exiting POST /filter");
			return Response.ok(o.toString()).build();
		} catch (Exception err) {
			logger.error(err.getMessage());
			return Response.serverError().build();
		} finally {
			EMF.closeEntityManager(em);
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
	@Produces("application/json")
	@Path("/authenticate")
	public Response authenticate(JSONObject userData) throws Exception {
		logger.debug("Entering post/authenticate");

		if (!userData.has("username") || !userData.has("password")) {
			logger.debug("Username or password missing in the JSON document.");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		EntityManager em = EMF.createEntityManager();
		try {
			User user = new UserDAO(em).findByUsername((String) userData
					.get("username"));
			if (user == null) {
				// User does not exist
				logger.debug("User not found. Resonse is UNAUTHORIZED");
				return Response.status(Response.Status.UNAUTHORIZED).build();
			}
			// Check that an unencrypted password matches one that has
			// previously been hashed
			if (BCrypt.checkpw((String) userData.get("password"),
					user.getPassword())) {
				logger.debug("Found user and password maches!");
			} else {
				logger.debug("Found the user but passwords do no match. ");
				return Response.status(Response.Status.NOT_ACCEPTABLE).build();
			}

			// Return user data
			JSONObject o = new JSONObject();
			o.put("username", user.getUsername());
			o.put("uri", RestUriBuilder.getUserUri(user));
			o.put("uuid", user.getUserId());
			logger.debug("Exiting get with return data: " + o.toString());
			return Response.ok(o.toString()).build();
			// URI resourceUri = new URI(String.format("/%d",user.getUserId()));
			// logger.debug("Exiting post");
			// return Response.ok().build();
		} catch (Exception err) {
			logger.error(err.getMessage());
			return Response.serverError().build();
		} finally {
			EMF.closeEntityManager(em);
		}
	}

	/**
	 * Authentication of users against the database with external identity.
	 * Consumes JSON document: POST /users/authenticateExt { attributes}
	 * 
	 * @param userData
	 *            holds list of attributes as JSON document
	 * @return user details as JSON document or "resource not exists"
	 */
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	@Path("/authenticateExt")
	public Response authenticateExt(JSONObject userData) throws Exception {
		logger.debug("Entering post/authenticateExt");
		// At least one attribute must map to a user and identityProvider
		// All attributes must map (if it maps) to the same user and
		// identityProvider
		// return that user
		//

		EntityManager em = EMF.createEntityManager();
		try {
			logger.debug("Iterating through keys");
			String strKey = null;
			String strVal = null;
			while (userData.keys().hasNext()) {
				strKey = ((org.json.JSONObject) userData.keys().next())
						.toString();
				strVal = (String) userData.get(strKey);
				logger.debug("Iterating through key " + strKey + ", val "
						+ strVal);
				Attribute attr = new AttributeDAO(em).findByName(strKey);
				if (attr == null) {
					logger.debug("Skipping key " + strKey + " since it is null ");
                    continue;
				}
				Query query = em
						.createNamedQuery("UserHasAttribute.findByAttributeId");
				query.setParameter("attributeId", attr.getAttributeId());
				if ((Long) query.getSingleResult() > 0) {
					// resource is already registered
					return Response.status(Response.Status.CONFLICT).build();
				}
			}

			if (!userData.has("username") || !userData.has("password")) {
				logger.debug("Username or password missing in the JSON document.");
				return Response.status(Response.Status.NOT_ACCEPTABLE).build();
			}

			User user = new UserDAO(em).findByUsername((String) userData
					.get("username"));
			if (user == null) {
				// User does not exist
				logger.debug("User not found. Resonse is UNAUTHORIZED");
				return Response.status(Response.Status.UNAUTHORIZED).build();
			}
			// Check that an unencrypted password matches one that has
			// previously been hashed
			if (BCrypt.checkpw((String) userData.get("password"),
					user.getPassword())) {
				logger.debug("Found user and password maches!");
			} else {
				logger.debug("Found the user but passwords do no match. ");
				return Response.status(Response.Status.NOT_ACCEPTABLE).build();
			}

			// Return user data
			String uri = String.format("/users/%d", user.getUserId());
			org.json.JSONObject o = new org.json.JSONObject();
			o.put("username", user.getUsername());
			o.put("uri", uri);
			logger.debug("Exiting get with return data: " + o.toString());
			return Response.ok(o.toString()).build();
			// URI resourceUri = new URI(String.format("/%d",user.getUserId()));
			// logger.debug("Exiting post");
			// return Response.ok().build();
		} catch (Exception err) {
			logger.error(err.getMessage());
			return Response.serverError().build();
		} finally {
			EMF.closeEntityManager(em);
		}
	}

	/**
	 * Creates user profile based on JSON attribute.
	 * 
	 * @param content
	 *            holds list of attributes as JSON document
	 * @return user details as JSON document or "resource not exists"
	 */
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	@Path("/createExt")
	public Response createExt(String content) throws Exception {
		// Create Generic user
		// Create identity provider with passed name. Can also be used generic
		// one.
		// Create User_has_identityProvider
		// Go through attributes. Create each attribute if it does not exist.
		// Update attribute with value and User_has_identityProvider.
		//
		return null;
	}

	/**
	 * Associates a user profile with external user attributes.
	 * 
	 * @param content
	 *            holds list of attributes, identityproviderURI and userId as
	 *            JSON document.
	 * @return OK on success
	 */
	@POST
	@Consumes("application/json")
	@Path("/associateExt")
	public Response associateExt(String content) throws Exception {
		// userId must be passed with JSON
		// all attributes must be added into Attribute, updated the values
		// User_has_identityProvider must be created, updated with attribute
		// values and the user
		// IdentityProvider must be added (or generic used).
		//
		return null;
	}

	/**
	 * As defined with Paolo Mori, email on WP7 Contrail mailing list, Date:
	 * Thu, 16 Feb 2012 11:35:29 +0100
	 */
	public static String SAML_ATTRIBUTES_USER_ROLE = "urn:contrail:names:federation:subject:role";
	public static String SAML_ATTRIBUTES_USER_GROUP = "urn:contrail:names:federation:subject:group";

	/**
	 * Creates a new entry in a collection.
	 * 
	 * @return
	 */
	@POST
	@Consumes("application/soap+xml")
	@Produces("application/soap+xml")
	@Path("/saml")
	public Response postSamlQuery(String theSOAPMsg) throws Exception {
		logger.debug("Entering postSamlQuery");
		org.opensaml.saml2.core.Response response = null;
		SAML.initialize();
		Envelope env = SAML.create(Envelope.class,
				Envelope.DEFAULT_ELEMENT_NAME);
		EntityManager em = null;
		try {
			logger.debug("SAML read:" + theSOAPMsg);
			logger.debug("Entering readSAMLQuery");
			AttributeQueryImpl objQueryImpl = null;
			try {
				objQueryImpl = (AttributeQueryImpl) SAML
						.readSAMLFromSOAPStr(theSOAPMsg);
			} catch (Exception err) {
				logger.error(err.getMessage());
			}
			logger.debug("Entering readSAMLQuery");
			String attributeQueryID = objQueryImpl.getID();
			logger.debug("Got attributeQueryID: " + attributeQueryID);
			String userUuid = objQueryImpl.getSubject().getNameID().getValue();
			logger.debug("Got NameID: " + userUuid);
			ArrayList<org.opensaml.saml2.core.Attribute> attributeList = new ArrayList<org.opensaml.saml2.core.Attribute>();
			for (org.opensaml.saml2.core.Attribute attr : objQueryImpl
					.getAttributes()) {
				logger.debug("got attribute: " + attr.getName());
				logger.debug("Attribute val:" + attr.getFriendlyName());
				attributeList.add(attr);
			}

			em = EMF.createEntityManager();

			try {
                User user = em.find(User.class, userUuid);
				if (user == null) {
					logger.error("No user is found.");
					Document document = SAML.asDOMDocument(env);
					String result = PrettyPrinter.prettyPrint(document);
					return Response.ok(result).build();
				}

				logger.debug("Obtaining attributes.");
				HashMap<String, String> attrList = new HashMap<String, String>();
				// There is no such problem which can not be resolved with
				// two loops.
				boolean allAttributes = false;
				if (objQueryImpl.getAttributes() == null
						|| objQueryImpl.getAttributes().size() == 0) {
					logger.debug("Attribute request containts listing of all attributes.");
					allAttributes = true;
				}
				boolean roles = false;
				boolean groups = false;
				for (org.opensaml.saml2.core.Attribute tempattr : objQueryImpl
						.getAttributes()) {
					if (tempattr.getName().equals("roles")) {
						logger.debug("Adding roles");
						roles = true;
					}
					if (tempattr.getName().equals("groups")) {
						logger.debug("Adding groups");
						groups = true;
					}
				}
				if (allAttributes) {
					roles = true;
					groups = true;
					logger.debug("Since allAttributes is true adding groups and roles.");
				}

				// Attributes
				for (UserHasAttribute userAttr : user.getUserHasAttributeList()) {
                    String attrName = userAttr.getAttribute().getName();
					if (!allAttributes) {
						for (org.opensaml.saml2.core.Attribute tempattr : objQueryImpl
								.getAttributes()) {
							if (tempattr.getName().equals(attrName)) {
								logger.debug("Adding attribute to the return list: " + attrName);
								logger.debug("Adding attribute's value to the return list:" + userAttr.getValue());
								attrList.put(attrName, userAttr.getValue());
							}
						}
					} else {
						logger.debug("Adding attribute to the return list: " + attrName);
						attrList.put(attrName, userAttr.getValue());
					}
				}
				// Roles
				ArrayList<String> rolesList = new ArrayList<String>();
				if (roles) {
					logger.debug("Adding roles to the list");
					for (Role role : user.getRoleList()) {
						rolesList.add(role.getName());
					}
					logger.debug("Got roles: " + rolesList.toString());
					// attrList.put("roles", rolesList.toString());
				}
				// Groups
				ArrayList<String> groupsList = new ArrayList<String>();
				if (groups) {
					logger.debug("Adding groups to the list");
					for (Group group : user.getGroupList()) {
						groupsList.add(group.getName());
					}
					logger.debug("Got groups: " + groupsList.toString());
					// attrList.put("groups", groupsList.toString());
				}
				logger.debug("Build response.");
				response = createSAMLResponse(userUuid, attributeQueryID,
						attrList, rolesList, groupsList);
			} finally {
				EMF.closeEntityManager(em);
			}
			if (response == null) {
				logger.error("Response equals null.");
				return Response.serverError().build();
			}
			logger.debug("Exiting doPost");
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.error(FederationDBCommon.getStackTrace(e));
		} 
		Body body = SAML.create(Body.class, Body.DEFAULT_ELEMENT_NAME);
		body.getUnknownXMLObjects().add(response);
		env.setBody(body);
		Document document = SAML.asDOMDocument(env);
		String result = PrettyPrinter.prettyPrint(document);
		return Response.ok(result).build();
	}

	/**
	 * Helper method to spawn a new Issuer element based on our issuer URL.
	 */
	public static Issuer spawnIssuer(String issuerURL) {
		Issuer result = null;
		if (issuerURL != null) {
			result = SAML.create(Issuer.class, Issuer.DEFAULT_ELEMENT_NAME);
			result.setValue(issuerURL);
		}
		return result;
	}

	/**
	 * Returns a SAML assertion with generated ID, current timestamp, given
	 * subject, and simple time-based conditions.
	 * 
	 * @param subject
	 *            Subject of the assertion
	 */
	public static Assertion createAssertion(Subject subject) throws Exception {
		logger.debug("Entering createAssertion");
		Assertion assertion = SAML.create(Assertion.class,
				Assertion.DEFAULT_ELEMENT_NAME);
		assertion.setID(SAML.getGenerator().generateIdentifier());

		DateTime now = new DateTime();
		assertion.setIssueInstant(now);
		assertion.setIssuer(spawnIssuer(MyServletContextListener
				.getConfProperty("oauthClient.id")));
		logger.debug("Issuer set to "
				+ MyServletContextListener.getConfProperty("oauthClient.id"));
		assertion.setSubject(subject);

		Conditions conditions = SAML.create(Conditions.class,
				Conditions.DEFAULT_ELEMENT_NAME);
		conditions.setNotBefore(now.minusSeconds(10));
		conditions.setNotOnOrAfter(now.plusMinutes(30));
		assertion.setConditions(conditions);
		logger.debug("Exiting createAssertion");
		return assertion;
	}

	/**
	 * Returns a SAML attribute assertion.
	 * 
	 * @param subject
	 *            Subject of the assertion
	 * @param attributes
	 *            Attributes to be stated (may be null)
	 */
	public static Assertion createAttributeAssertion(Subject subject,
			HashMap<String, String> attributes, ArrayList<String> rolesList,
			ArrayList<String> groupsList) throws Exception {
		logger.debug("Entering createAttributeAssertion");
		Assertion assertion = createAssertion(subject);
		AttributeStatement statement = SAML.create(AttributeStatement.class,
				AttributeStatement.DEFAULT_ELEMENT_NAME);
		if (attributes != null)
			for (Map.Entry<String, String> entry : attributes.entrySet())
				SAML.addAttribute(statement, entry.getKey(), entry.getValue());
		if (rolesList != null)
			SAML.addAttributeMultipleValues(statement,
					SAML_ATTRIBUTES_USER_ROLE, rolesList);
		if (groupsList != null)
			SAML.addAttributeMultipleValues(statement,
					SAML_ATTRIBUTES_USER_GROUP, groupsList);
		assertion.getStatements().add(statement);
		logger.debug("Exiting createAttributeAssertion");
		return assertion;
	}

	public static org.opensaml.saml2.core.Response createSAMLResponse(
			String nameid, String inResposeTo,
			HashMap<String, String> attrList, ArrayList<String> rolesList,
			ArrayList<String> groupsList) throws Exception,
			MarshallingException, TransformerException {
		logger.debug("Entering createSAMLResponse");
		Subject subject = SAML.createSubject(nameid, NameID.TRANSIENT, null);
		Assertion assertion = createAttributeAssertion(subject, attrList,
				rolesList, groupsList);
		org.opensaml.saml2.core.Response response = SAML
				.createResponse(assertion);
		response.setIssuer(spawnIssuer(MyServletContextListener
				.getConfProperty("oauthClient.id")));
		response.setInResponseTo(inResposeTo);
		logger.debug("Exiting createSAMLResponse");
		return response;
	}

}

enum SearchFilter {
	USER("user"), USER_USERNAME("user:username"), USER_ATTRIBUTE(
			"user:attribute"), USER_UUID("user:uuid"), USER_DBID("user:id"), USER_FIRST_NAME(
			"user:firstName"), USER_LAST_NAME("user:lastName"), USER_IDP_IDENTITY(
			"user:idp:identity"), USER_EMAIL("user:email");

	SearchFilter(String value) {
		this.value = value;
	}

	private String value;

	public String value() {
		return this.value;
	}
}

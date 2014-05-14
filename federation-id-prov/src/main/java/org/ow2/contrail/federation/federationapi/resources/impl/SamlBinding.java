/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.AttributeQueryImpl;
import org.opensaml.ws.soap.soap11.Body;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.xml.io.MarshallingException;
import org.ow2.contrail.federation.federationapi.MyServletContextListener;
import org.ow2.contrail.federation.federationapi.saml.PrettyPrinter;
import org.ow2.contrail.federation.federationapi.saml.SAML;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationdb.jpa.dao.AttributeDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.UGroup;
import org.ow2.contrail.federation.federationdb.jpa.entities.URole;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserhasAttribute;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;
import org.w3c.dom.Document;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ales.cernivec@xlab.si
 */
@Path("/saml")
public class SamlBinding {

    protected static Logger logger =
            Logger.getLogger(SamlBinding.class);

    /**
     * As defined with Paolo Mori, email on WP7 Contrail mailing list, Date: Thu, 16 Feb 2012 11:35:29 +0100
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
    public Response postSamlQuery(String theSOAPMsg) throws Exception {
        logger.debug("Entering postSamlQuery");
        org.opensaml.saml2.core.Response response = null;
        SAML.initialize();
        Envelope env = SAML.create(Envelope.class, Envelope.DEFAULT_ELEMENT_NAME);
        try {
            logger.debug("SAML read:" + theSOAPMsg);
            logger.debug("Entering readSAMLQuery");
            AttributeQueryImpl objQueryImpl = null;
            try {
                objQueryImpl =
                        (AttributeQueryImpl) SAML.readSAMLFromSOAPStr(theSOAPMsg);
            }
            catch (Exception err) {
                logger.error(err.getMessage());
            }
            logger.debug("Entering readSAMLQuery");
            String attributeQueryID = objQueryImpl.getID();
            logger.debug("Got attributeQueryID: " + attributeQueryID);
            String NameID = objQueryImpl.getSubject().getNameID().getValue();
            logger.debug("Got NameID: " + NameID);
            ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
            for (Attribute attr : objQueryImpl.getAttributes()) {
                logger.debug("got attribute: " + attr.getName());
                logger.debug("Attribute val:" + attr.getFriendlyName());
                attributeList.add(attr);
            }

            EntityManager em = PersistenceUtils.getInstance().getEntityManager();
            User user = null;

            try {
                Query query = em.createNamedQuery("User.findByUsername");
                query.setParameter("username", NameID);
                List<User> usrList = query.getResultList();
                if (usrList.size() == 0) {
                    logger.error("No user is found.");
                    Document document = SAML.asDOMDocument(env);
                    String result = PrettyPrinter.prettyPrint(document);
                    return Response.ok(result).build();
                }
                if (usrList.size() != 1) {
                    logger.error("More than one user with that ID found.");
                    Document document = SAML.asDOMDocument(env);
                    String result = PrettyPrinter.prettyPrint(document);
                    return Response.ok(result).build();
                }
                user = usrList.get(0);
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
                if (objQueryImpl.getAttributes() == null || objQueryImpl.getAttributes().size() == 0) {
                    logger.debug("Attribute request containts listing of all attributes.");
                    allAttributes = true;
                }
                boolean roles = false;
                boolean groups = false;
                for (Attribute tempattr : objQueryImpl.getAttributes()) {
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
                for (UserhasAttribute attr : user.getUserhasAttributeList()) {
                    org.ow2.contrail.federation.federationdb.jpa.entities.Attribute usrAttr = AttributeDAO.findById(attr.getUserhasAttributePK().getAttributeId());
                    if (!allAttributes) {
                        for (Attribute tempattr : objQueryImpl.getAttributes()) {
                            if (tempattr.getName().equals(usrAttr.getName())) {
                                logger.debug("Adding attribute to the return list:" + usrAttr.getName());
                                logger.debug("Adding attribute's value to the return list:" + attr.getValue());
                                attrList.put(usrAttr.getName(), attr.getValue());
                            }
                        }
                    }
                    else {
                        logger.debug("Adding attribute to the return list:" + usrAttr.getName());
                        attrList.put(usrAttr.getName(), attr.getValue());
                    }
                }
                // Roles
                ArrayList<String> rolesList = new ArrayList<String>();
                if (roles) {
                    logger.debug("Adding roles to the list");
                    for (URole role : user.getURoleList()) {
                        rolesList.add(role.getName());
                    }
                    logger.debug("Got roles: " + rolesList.toString());
                    //attrList.put("roles", rolesList.toString());
                }
                // Groups
                ArrayList<String> groupsList = new ArrayList<String>();
                if (groups) {
                    logger.debug("Adding groups to the list");
                    for (UGroup group : user.getUGroupList()) {
                        groupsList.add(group.getName());
                    }
                    logger.debug("Got groups: " + groupsList.toString());
                    //attrList.put("groups", groupsList.toString());
                }
                logger.debug("Build response.");
                response = createSAMLResponse(NameID, attributeQueryID, attrList, rolesList, groupsList);
            }
            finally {
                PersistenceUtils.getInstance().closeEntityManager(em);
            }
            if (response == null) {
                logger.error("Response equals null.");
                return Response.serverError().build();
            }
            logger.debug("Exiting doPost");
        }
        catch (Exception e) {
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
     * @param subject Subject of the assertion
     */
    public static Assertion createAssertion(Subject subject) {
        logger.debug("Entering createAssertion");
        Assertion assertion =
                SAML.create(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setID(SAML.getGenerator().generateIdentifier());

        DateTime now = new DateTime();
        assertion.setIssueInstant(now);
        assertion.setIssuer(spawnIssuer(MyServletContextListener.getServerName()));
        assertion.setSubject(subject);

        Conditions conditions = SAML.create(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(now.minusSeconds(10));
        conditions.setNotOnOrAfter(now.plusMinutes(30));
        assertion.setConditions(conditions);
        logger.debug("Exiting createAssertion");
        return assertion;
    }

    /**
     * Returns a SAML attribute assertion.
     *
     * @param subject    Subject of the assertion
     * @param attributes Attributes to be stated (may be null)
     */
    public static Assertion createAttributeAssertion(
            Subject subject,
            HashMap<String, String> attributes,
            ArrayList<String> rolesList,
            ArrayList<String> groupsList) {
        logger.debug("Entering createAttributeAssertion");
        Assertion assertion = createAssertion(subject);
        AttributeStatement statement = SAML.create(AttributeStatement.class,
                AttributeStatement.DEFAULT_ELEMENT_NAME);
        if (attributes != null)
            for (Map.Entry<String, String> entry : attributes.entrySet())
                SAML.addAttribute(statement, entry.getKey(), entry.getValue());
        if (rolesList != null)
            SAML.addAttributeMultipleValues(statement, SAML_ATTRIBUTES_USER_ROLE, rolesList);
        if (groupsList != null)
            SAML.addAttributeMultipleValues(statement, SAML_ATTRIBUTES_USER_GROUP, groupsList);
        assertion.getStatements().add(statement);
        logger.debug("Exiting createAttributeAssertion");
        return assertion;
    }

    public static org.opensaml.saml2.core.Response createSAMLResponse(
            String nameid,
            String inResposeTo,
            HashMap<String, String> attrList,
            ArrayList<String> rolesList,
            ArrayList<String> groupsList
    )
            throws IOException, MarshallingException, TransformerException {
        logger.debug("Entering createSAMLResponse");
        Subject subject = SAML.createSubject(nameid, NameID.TRANSIENT, null);
        Assertion assertion = createAttributeAssertion(subject, attrList, rolesList, groupsList);
        org.opensaml.saml2.core.Response response = SAML.createResponse(assertion);
        response.setIssuer(spawnIssuer(MyServletContextListener.getServerName()));
        response.setInResponseTo(inResposeTo);
        logger.debug("Exiting createSAMLResponse");
        return response;
    }
}


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.contrail.security.servercommons;

import org.mindrot.jbcrypt.BCrypt;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.impl.AssertionMarshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.XMLHelper;
import org.ow2.contrail.federation.federationdb.jpa.dao.AttributeDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.*;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author ijj
 */
public class UserSAML {

    public HashMap<String, String> getAllAttributes(final User user) {

        HashMap<String, String> attrList = new HashMap<String, String>();
        for (UserhasAttribute attr : user.getUserhasAttributeList()) {

            Attribute usrAttr = new AttributeDAO().findByUuid(attr.getUserhasAttributePK().getAttributeUuid());
            attrList.put(usrAttr.getName(), attr.getValue());

        }
        return attrList;
    }

    public User getUser(final EntityManager em, final String username, final String password) throws NoResultException, NonUniqueResultException {
        User user;
        final String queryString = "SELECT u FROM User u WHERE u.username = :username";
        Query query = em.createQuery(queryString);
        query.setParameter("username", username);
        user = (User) query.getSingleResult();
        if (user == null) {
            throw new NoResultException("NULL result from SQL query");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new NoResultException("Username and password do not match");
        }
        return user;
    }

    public String getAttributeValue(final Node attributeStatement, final String attributeName) {
        String value = null;
        NodeList attributeValues = attributeStatement.getChildNodes();
        System.err.printf("attributeValues has size %d.\n", attributeValues.getLength());
        if (attributeValues != null) {
            for (int i = 0; i < attributeValues.getLength(); i++) {
                Node n = attributeValues.item(i);
                NamedNodeMap map = n.getAttributes();
                if (map == null) {
                    //         System.err.printf("NamedNodeMap is NULL");
                    continue;
                }
                Node nn = map.getNamedItem("Name");
                if (nn != null && attributeName.equals(nn.getNodeValue())) {
                    String content = n.getTextContent().trim();
                    value = content;
                    break;
                }
            }
        }
        return value;
    }

    public String getURoleList(User user) {
        List<URole> roleList = user.getURoleList();
        Iterator<URole> iterRole = roleList.iterator();
        StringBuilder roles = new StringBuilder();
        boolean first = true;
        while (iterRole.hasNext()) {
            String roleName = iterRole.next().getName();
            roles.append(first ? "" : ",").append(roleName);
            first = false;
        }
        return roles.toString();
    }

    public String getGroupList(User user) {
        List<UGroup> groupList = user.getUGroupList();
        Iterator<UGroup> iterGroup = groupList.iterator();
        StringBuilder groups = new StringBuilder();
        boolean first = true;
        while (iterGroup.hasNext()) {
            String groupName = iterGroup.next().getName();
            groups.append(first ? "" : ",").append(groupName);
            first = false;
        }
        return groups.toString();
    }

//  public interface Nameable {
//    
//    public String getName();
//    
//  }
//  
//  public <T> String getListValues(Iterator<T> iter) {
// 
//    StringBuilder bldr = null;
//    
//    boolean first = true;
//    
//    while (iter.hasNext()) {
//      
//      String name = ((T)iter.next()).getName();
//      bldr.append(first ? "" : ",").append(name);
//    }
//    
//    return bldr.toString();
//  }

    public HashMap<String, String> getUserAttributes(User user) {
        String roles = getURoleList(user);
        String groups = getGroupList(user);

        String uuid = user.getUuid();

        HashMap<String, String> attributes = this.getAllAttributes(user);
        if (!attributes.isEmpty()) {
            attributes.put("urn:contrail:names:federation:subject:role", roles);
            attributes.put("urn:contrail:names:federation:subject:group", groups);
            attributes.put("urn:contrail:names:federation:subject:uuid", uuid);
        }
        return attributes;
    }

    public Assertion createUserAssertion(User user, SAML saml, HashMap<String, String> attributes) {
        final String nameId = user.getUsername();
        Subject subject = SAML.createSubject(nameId, NameID.TRANSIENT, null);
        Assertion assertion = saml.createAttributeAssertion(subject, attributes);
        return assertion;
    }

    public String getXMLAssertion(Assertion assertion) {

        String xml = null;

        AssertionMarshaller marshaller = new AssertionMarshaller();
        Element element = null;
        try {
            element = marshaller.marshall(assertion);
        }
        catch (MarshallingException ex) {
        }
        xml = XMLHelper.prettyPrintXML(element);

        return xml;
    }

    public String getSAMLforUser(User user, SAML saml) {

        String xml = null;

        HashMap<String, String> attributes = getUserAttributes(/* this */ user);
        Assertion assertion = createUserAssertion(user, saml, attributes);
        xml = getXMLAssertion(assertion);

        return xml;

    }


    public User getUserbyUserID(final EntityManager em, final String userID)
            throws NoResultException, NumberFormatException {

        User user = null;

        TypedQuery<User> query = em.createNamedQuery("User.findByUserId", User.class);
        query.setParameter("userId", Integer.valueOf(userID));

        user = query.getSingleResult();

        if (user == null) {

            throw new NoResultException();

        }

        return user;

    }

}

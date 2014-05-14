/**
 *
 */
package org.ow2.contrail.federation.federationapi;

import org.apache.log4j.Logger;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.impl.AttributeQueryImpl;
import org.opensaml.ws.soap.soap11.Body;
import org.opensaml.ws.soap.soap11.Envelope;
import org.ow2.contrail.federation.federationapi.resources.impl.SamlBinding;
import org.ow2.contrail.federation.federationapi.saml.PrettyPrinter;
import org.ow2.contrail.federation.federationapi.saml.SAML;
import org.ow2.contrail.federation.federationdb.jpa.dao.AttributeDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserhasAttribute;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author ales
 */
public class SOAPTest {

    protected static Logger logger =
            Logger.getLogger(SOAPTest.class);

    public static void samlTest(String theSOAPMsg) {
        logger.debug("Entering postSamlQuery");
        org.opensaml.saml2.core.Response response = null;
        try {
            System.out.println("SAML read:" + theSOAPMsg);
            logger.debug("Entering readSAMLQuery");
            SAML saml = new SAML();
            AttributeQueryImpl objQueryImpl = null;
            try {
                objQueryImpl =
                        (AttributeQueryImpl) saml.readSAMLFromSOAPStr(theSOAPMsg);
            }
            catch (Exception err) {
                logger.error(err.getMessage());
            }
            logger.debug("Entering readSAMLQuery");
            String NameID = objQueryImpl.getSubject().getNameID().getValue();
            logger.debug("Got NameID: " + NameID);
            for (Attribute attr : objQueryImpl.getAttributes()) {
                logger.debug("got attribute: " + attr.getName());
                logger.debug("Attribute val:" + attr.getFriendlyName());
            }

            EntityManager em = PersistenceUtils.getInstance().getEntityManager();
            User user = null;

            try {
                Query query = em.createNamedQuery("User.findByUsername");
                query.setParameter("username", NameID);
                List<User> usrList = query.getResultList();
                if (usrList.size() != 1) {
                    logger.error("More than one user with that ID found.");
                }
                user = usrList.get(0);
                if (user == null) {
                    logger.error("No user is found.");

                }
                logger.debug("Obtaining attributes.");
                HashMap<String, String> attrList = new HashMap<String, String>();
                for (UserhasAttribute attr : user.getUserhasAttributeList()) {
                    org.ow2.contrail.federation.federationdb.jpa.entities.Attribute usrAttr = AttributeDAO.findById(attr.getUserhasAttributePK().getAttributeId());
                    logger.debug("Adding attribute to the return list:" + usrAttr.getName());
                    logger.debug("Adding attribute's value to the return list:" + attr.getValue());
                    attrList.put(usrAttr.getName(), attr.getValue());
                }
                logger.debug("Build response.");
                response = SamlBinding.createSAMLResponse(NameID, NameID, attrList, new ArrayList<String>(), new ArrayList<String>());
            }
            finally {
                PersistenceUtils.getInstance().closeEntityManager(em);
            }
            if (response == null) {
                logger.error("Response equals null.");

            }
            logger.debug("Exiting doPost");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Body body = SAML.create(Body.class, Body.DEFAULT_ELEMENT_NAME);
        body.getUnknownXMLObjects().add(response);
        Envelope env = SAML.create(Envelope.class, Envelope.DEFAULT_ELEMENT_NAME);
        env.setBody(body);
        String result = null;
        try {
            Document document = SAML.asDOMDocument(env);
            result = PrettyPrinter.prettyPrint(document);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
        }
        System.out.println(result);
    }

    private static String readFileAsString(String filePath) throws java.io.IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        }
        finally {
            if (f != null) try {
                f.close();
            }
            catch (IOException ignored) {
            }
        }
        return new String(buffer);
    }

    private void parseSOAP() {
        //get the factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document dom = null;
        try {

            //Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();

            //parse using builder to get DOM representation of the XML file
            dom = db.parse(new File("soap.xml"));
            Element docEle = dom.getDocumentElement();
            NodeList nl = docEle.getElementsByTagName("soap11:Body");

            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {

                    //get the employee element
                    Element el = (Element) nl.item(i);
                    String val = el.getFirstChild().getNodeValue();
                    Object obj = el.getUserData("saml:Issuer");
                    System.out.println(el.getBaseURI());
                }
            }

        }
        catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        }
        catch (SAXException se) {
            se.printStackTrace();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String soapStr = readFileAsString("soap-attr.xml");
        samlTest(soapStr);
    }

}

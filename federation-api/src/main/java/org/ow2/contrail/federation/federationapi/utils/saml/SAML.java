package org.ow2.contrail.federation.federationapi.utils.saml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import org.opensaml.DefaultBootstrap;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.schema.XSInteger;
import org.opensaml.xml.schema.XSString;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * @author ales
 *
 */
public class SAML
{

	protected static Logger logger =
		Logger.getLogger(SAML.class);

	private static DocumentBuilder builder;
	private static String issuerURL;

	private static SecureRandomIdentifierGenerator generator;
	private static final String CM_PREFIX = "urn:oasis:names:tc:SAML:2.0:cm:";

	public static void initialize(){
		SAML.initialize(null);
	}

	public static void initialize(String issuerURL){
		try
		{
			DocumentBuilderFactory factory = 
				DocumentBuilderFactory.newInstance ();
			factory.setNamespaceAware (true);
			builder = factory.newDocumentBuilder ();
			SAML.issuerURL = issuerURL;
		}
		catch (Exception ex)
		{
			ex.printStackTrace ();
		}
	}

	/**
    Any use of this class assures that OpenSAML is bootstrapped.
    Also initializes an ID generator.
	 */
	static 
	{
		try
		{
			DefaultBootstrap.bootstrap ();
			generator = new SecureRandomIdentifierGenerator ();
		}
		catch (Exception ex)
		{
			ex.printStackTrace ();
		}
	}

	/**
    <u>Slightly</u> easier way to create objects using OpenSAML's 
    builder system.
	 */
	// cast to SAMLObjectBuilder<T> is caller's choice    
	@SuppressWarnings ("unchecked")
	public static <T> T create (Class<T> cls, QName qname)
	{
		return (T) ((XMLObjectBuilder) 
				Configuration.getBuilderFactory ().getBuilder (qname))
				.buildObject (qname);
	}

	/**
    Helper method to add an XMLObject as a child of a DOM Element.
	 */
	public static Element addToElement (XMLObject object, Element parent)
	throws IOException, MarshallingException, TransformerException
	{
		Marshaller out = 
			Configuration.getMarshallerFactory ().getMarshaller (object);
		return out.marshall (object, parent);
	}

	/**
    Helper method to get an XMLObject as a DOM Document.
	 */
	public static Document asDOMDocument (XMLObject object)
	throws IOException, MarshallingException, TransformerException
	{
		Document document = builder.newDocument ();
		Marshaller out = 
			Configuration.getMarshallerFactory ().getMarshaller (object);
		out.marshall (object, document);
		return document;
	}

	/**
    Helper method to pretty-print any XML object to a file.
	 */
	public void printToFile (XMLObject object, String filename)
	throws IOException, MarshallingException, TransformerException
	{
		Document document = asDOMDocument (object);

		String result = PrettyPrinter.prettyPrint (document);
		if (filename != null)
		{
			PrintWriter writer = new PrintWriter (new FileWriter (filename));
			writer.println (result);
			writer.close ();
		}
		else
			System.out.println (result);
	}

	/**
    Helper method to read an XML object from a DOM element.
	 */
	public static XMLObject fromElement (Element element)
	throws IOException, UnmarshallingException, SAXException
	{
		return Configuration.getUnmarshallerFactory ()
		.getUnmarshaller (element).unmarshall (element);    
	}

	/**
	 * 
	 * @param str
	 * @return
	 * @throws IOException
	 * @throws UnmarshallingException
	 * @throws SAXException
	 */
	public static XMLObject readFromString (String str)
	throws IOException, UnmarshallingException, SAXException
	{
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(str.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
		return fromElement (builder.parse (is).getDocumentElement ());

	}

	/**
	 * Helper method to read an SAML Attribute Query object from a SOAP message.
	 * @param str SOAP message as String.
	 * @return {@link RequestAbstractType} SAML Attribute Query object 
	 * 
	 */
	public static XMLObject readSAMLFromSOAPStr (String str)
	throws IOException, UnmarshallingException, SAXException
	{
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(str.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
		Envelope env = null;
		try
		{
			env = (Envelope)readFromString (str);
			for (XMLObject object : env.getBody ().getUnknownXMLObjects ())
				if (object instanceof RequestAbstractType)
					return (RequestAbstractType)object;
		}
		catch (Exception ex)
		{
			System.err.println ("Couldn't read corresponding query file; " +
			"InResponseTo will be missing.");
		}
		return env;
	}

	/**
    Helper method to spawn a new Issuer element based on our issuer URL.
	 */
	public static Issuer spawnIssuer ()
	{
		Issuer result = null;
		if (issuerURL != null)
		{
			result = create (Issuer.class, Issuer.DEFAULT_ELEMENT_NAME);
			result.setValue (issuerURL);
		}

		return result;
	}

	/**
    Returns a SAML subject.

    @param username The subject name
    @param format If non-null, we'll set as the subject name format
    @param confirmationMethod If non-null, we'll create a SubjectConfirmation
        element and use this as the Method attribute; must be "sender-vouches"
        or "bearer", as HOK would require additional parameters and so is NYI
	 */
	public static Subject createSubject
	(String username, String format, String confirmationMethod)
	{
		NameID nameID = create (NameID.class, NameID.DEFAULT_ELEMENT_NAME);
		nameID.setValue (username);
		if (format != null)
			nameID.setFormat (format);

		Subject subject = create (Subject.class, Subject.DEFAULT_ELEMENT_NAME);
		subject.setNameID (nameID);

		if (confirmationMethod != null)
		{
			SubjectConfirmation confirmation = create 
			(SubjectConfirmation.class, 
					SubjectConfirmation.DEFAULT_ELEMENT_NAME);
			confirmation.setMethod (CM_PREFIX + confirmationMethod);

			subject.getSubjectConfirmations ().add (confirmation);
		}

		return subject;        
	}

	/**
    Returns a SAML assertion with generated ID, current timestamp, given
    subject, and simple time-based conditions.

    @param subject Subject of the assertion
	 */
	public Assertion createAssertion (Subject subject)
	{
		Assertion assertion = 
			create (Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);
		assertion.setID (generator.generateIdentifier ());

		DateTime now = new DateTime ();
		assertion.setIssueInstant (now);

		if (issuerURL != null)
			assertion.setIssuer (spawnIssuer ());

		assertion.setSubject (subject);

		Conditions conditions = create 
		(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);
		conditions.setNotBefore (now.minusSeconds (10));
		conditions.setNotOnOrAfter (now.plusMinutes (30));
		assertion.setConditions (conditions);

		return assertion;
	}

	/**
    Helper method to generate a response, based on a pre-built assertion.
	 */
	public static Response createResponse (Assertion assertion)
	throws IOException, MarshallingException, TransformerException
	{
		return createResponse (assertion, null);
	}

	/**
    Helper method to generate a shell response with a given status code
    and query ID.
	 */
	public static Response createResponse (String statusCode, String inResponseTo)
	throws IOException, MarshallingException, TransformerException
	{
		return createResponse (statusCode, null, inResponseTo);
	}

	/**
    Helper method to generate a shell response with a given status code,
    status message, and query ID.
	 */
	public static Response createResponse 
	(String statusCode, String message, String inResponseTo)
	throws IOException, MarshallingException, TransformerException
	{
		Response response = create 
		(Response.class, Response.DEFAULT_ELEMENT_NAME);
		response.setID (generator.generateIdentifier ());

		if (inResponseTo != null)
			response.setInResponseTo (inResponseTo);

		DateTime now = new DateTime ();
		response.setIssueInstant (now);

		if (issuerURL != null)
			response.setIssuer (spawnIssuer ());

		StatusCode statusCodeElement = create 
		(StatusCode.class, StatusCode.DEFAULT_ELEMENT_NAME);
		statusCodeElement.setValue (statusCode);

		Status status = create (Status.class, Status.DEFAULT_ELEMENT_NAME);
		status.setStatusCode (statusCodeElement);
		response.setStatus (status);

		if (message != null)
		{
			StatusMessage statusMessage = create 
			(StatusMessage.class, StatusMessage.DEFAULT_ELEMENT_NAME);
			statusMessage.setMessage (message);
			status.setStatusMessage (statusMessage);
		}

		return response;
	}

	/**
    Helper method to generate a response, based on a pre-built assertion
    and query ID.
	 */
	public static Response createResponse (Assertion assertion, String inResponseTo)
	throws IOException, MarshallingException, TransformerException
	{
		Response response = 
			createResponse (StatusCode.SUCCESS_URI, inResponseTo);

		response.getAssertions ().add (assertion);

		return response;
	}

	/**
    Returns a SAML authentication assertion.

    @param subject The subject of the assertion
    @param authnCtx The "authentication context class reference",
      e.g. AuthnContext.PPT_AUTHN_CTX
	 */
	public Assertion createAuthnAssertion (Subject subject, String authnCtx)
	{
		Assertion assertion = createAssertion (subject);

		AuthnContextClassRef ref = create (AuthnContextClassRef.class, 
				AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
		ref.setAuthnContextClassRef (authnCtx);

		// As of this writing, OpenSAML doesn't model the wide range of
		// authentication context namespaces defined in SAML 2.0.
		// For a real project we'd probably move on to 
		//    XSAny objects, setting QNames and values each-by-each
		//    a JAXB mapping of the required schema
		//    DOM-building
		// For classroom purposes the road ends here ...

		AuthnContext authnContext = create 
		(AuthnContext.class, AuthnContext.DEFAULT_ELEMENT_NAME);
		authnContext.setAuthnContextClassRef (ref);

		AuthnStatement authnStatement = create 
		(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
		authnStatement.setAuthnContext (authnContext);

		assertion.getStatements ().add (authnStatement);

		return assertion;
	}

	/**
    Adds a SAML attribute to an attribute statement.

    @param statement Existing attribute statement
    @param name Attribute name
    @param value Attribute value
	 */
	public static void addAttribute 
	(AttributeStatement statement, String name, String value)
	{
		// Build attribute values as XMLObjects;
		//  there is an AttributeValue interface, but it's apparently dead code
		final XMLObjectBuilder builder = 
			Configuration.getBuilderFactory ().getBuilder ( XSAny.TYPE_NAME);

		XSAny valueElement = (XSAny) builder.buildObject 
		(AttributeValue.DEFAULT_ELEMENT_NAME);
		valueElement.setTextContent(value);

		Attribute attribute = create 
		(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
		attribute.setName (name);
		attribute.getAttributeValues ().add (valueElement);

		statement.getAttributes ().add (attribute);
	}
	
	/**
    Adds a SAML attribute with multiple values to an attribute statement.

    @param statement Existing attribute statement
    @param name Attribute name
    @param values ArrayList of string of Attribute values 
	 */
	public static void addAttributeMultipleValues 
	(AttributeStatement statement, String name, ArrayList<String> values)
	{
		final XMLObjectBuilder builder = Configuration.getBuilderFactory ().getBuilder ( XSAny.TYPE_NAME);
		Attribute attribute = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
		attribute.setName (name);
		for(String value: values){
			XSAny valueElement = (XSAny) builder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
			valueElement.setTextContent(value);
			attribute.getAttributeValues ().add (valueElement);
		}
		statement.getAttributes ().add (attribute);
	}

	/**
    Adds a SAML attribute to an attribute statement.

    @param type Attribute type. One of XSAny, XSString, XSInteger
    @param statement Existing attribute statement
    @param name Attribute name
    @param value Attribute value
	 */
	public static void addAttribute 
	(ContrailAttributeTypes type, AttributeStatement statement, String name, String value)
	{
		if(type == ContrailAttributeTypes.String){
			// Build attribute values as XMLObjects;
			//  there is an AttributeValue interface, but it's apparently dead code
			final XMLObjectBuilder builder = 
				Configuration.getBuilderFactory ().getBuilder ( XSString.TYPE_NAME /*XSAny.TYPE_NAME*/);

			XSString valueElement = (XSString) builder.buildObject 
			(AttributeValue.DEFAULT_ELEMENT_NAME);
			valueElement.setValue (value);

			Attribute attribute = create 
			(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
			attribute.setName (name);
			attribute.getAttributeValues ().add (valueElement);

			statement.getAttributes ().add (attribute);
		}
		else if(type == ContrailAttributeTypes.Integer){
			// Build attribute values as XMLObjects;
			//  there is an AttributeValue interface, but it's apparently dead code
			final XMLObjectBuilder builder = 
				Configuration.getBuilderFactory ().getBuilder ( XSInteger.TYPE_NAME /*XSAny.TYPE_NAME*/);

			XSInteger valueElement = (XSInteger) builder.buildObject 
			(AttributeValue.DEFAULT_ELEMENT_NAME);
			try{
				valueElement.setValue(Integer.parseInt(value));
			}catch(Exception err){
				logger.error(err.getMessage());				
			}
			Attribute attribute = create 
			(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
			attribute.setName (name);
			attribute.getAttributeValues ().add (valueElement);

			statement.getAttributes ().add (attribute);
		}
		else{
			logger.error("Attribute not in correct type.");
			logger.error("Not adding the attribute.");
		}
	}


	/**
    Returns a SAML attribute assertion.

    @param subject Subject of the assertion
    @param attributes Attributes to be stated (may be null)
	 */
	public Assertion createAttributeAssertion 
	(Subject subject, Map<String,String> attributes)
	{
		Assertion assertion = createAssertion (subject);

		AttributeStatement statement = create (AttributeStatement.class, 
				AttributeStatement.DEFAULT_ELEMENT_NAME);
		if (attributes != null)
			for (Map.Entry<String,String> entry : attributes.entrySet ())
				addAttribute (statement, entry.getKey (), entry.getValue ());

		assertion.getStatements ().add (statement);

		return assertion;
	}

	public static SecureRandomIdentifierGenerator getGenerator() {
		return generator;
	}

	public static void setGenerator(SecureRandomIdentifierGenerator generator) {
		SAML.generator = generator;
	}
}

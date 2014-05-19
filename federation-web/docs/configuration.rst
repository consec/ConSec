Configuration
=============

Configuration file
------------------

Here we describe configuration parameters.

Configuration resides here: */etc/contrail/contrail-federation-web/federation-web.conf*.

.. code-block:: python

	STATIC_ROOT = '/var/lib/contrail/federation/federation-web/static'
	
	FEDERATION_API_URL = 'http://localhost:8080/federation-api'
	SLA_EXTRACTOR_BASE = 'http://localhost:8080/rest-monitoring/sla/slaextractor'
	MONITORING_BASE = 'http://localhost:8080/rest-monitoring/monitoring'
	ZOOKEEPER_BASE = '127.0.0.1:2181'
	
	ONLINE_CA_USE=False
	ONLINE_CA_URI='https://one-test.contrail.rl.ac.uk:8443/ca/portaluser'
	CA_KEY_FILE = '/etc/contrail/client.key'
	CA_CERT_FILE = '/etc/contrail/client.crt'
	CA_CERTS_FILE = 'one-test-ssl-chain.pem'
	
	FEDERATION_WEB='http://localhost'
	FEDERATION_WEB_LOCAL_METADATA='/etc/contrail/contrail-federation-web/remote_metadata.xml'
	FEDERATION_WEB_CERT='/etc/contrail/contrail-federation-web/mycert.pem'
	FEDERATION_WEB_KEY='/etc/contrail/contrail-federation-web/mycert.key'
	
	FEDERATION_IDP_GOOGLE='http://google.contrail-idp.contrail.eu'
	FEDERATION_IDP_FEDERATION='http://federation.contrail-idp.contrail.eu'
	
Basic variables
^^^^^^^^^^^^^^^

**STATIC_ROOT** contains static files used by *contrail-federation-web*.

**FEDERATION_API_URL** points to URI where *contrail-federation-api* is installed.

**SLA_EXTRACTOR_BASE** points to URI of the *slaextractor* component.

**MONITORING_BASE** points to URI of the *monitoring* component.

**ZOOKEEPER_BASE** is where *zookeeper* is running.

Variables related to CA server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**ONLINE_CA_USE** are we using CA server? Can be *True* or *False*.

**ONLINE_CA_URI** if we are using CA server, this points to the URI of the CA server.

**CA_KEY_FILE** this is where client's key file is stored.

**CA_CERT_FILE** this is where client's certificate file is stored.

**CA_CERTS_FILE** chain of certificates used for secure communication with CA server.

Variables needed by Contrail Federation Identity Provider component (contrail-federation-id-prov)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**FEDERATION_WEB** must be URI of *contrail-federation-web* as seen to the outside's world.

**FEDERATION_WEB_LOCAL_METADATA** designates location of metadata that defines remote identity provider. 

**FEDERATION_WEB_CERT** is a path to *contrail-federation-web*'s certificate used for SSL.

**FEDERATION_WEB_KEY** is a path to *contrail-federation-web*'s private key used for SSL.

**FEDERATION_IDP_GOOGLE** points to URI towards *contrail-federation-id-prov*'s access point for OpenID authentication with Google.

**FEDERATION_IDP_FEDERATION** points to URI towards *contrail-federation-id-prov*'s access point for basic authentication with the Contrail DB.

Metadata file
-------------

Under configuration directory of the *contrail-federation-web* file *remote_metadata.xml* describes Contrail IdP access points for authentication. 
Since *contrail-federation-id-prov* currently provides two different authentication mechanisms:

 - authentication towards Contrail's DB
  
 - authentication via external identity provider (google)
 
we need a description of two access points. This description is used by *contrail-federation-web* to query user which mechanism she will use.

You must ensure that 

 - **entityID** 's hosts parts

 - **Location** 's hosts parts

are the same as **FEDERATION_IDP_GOOGLE** and **FEDERATION_IDP_FEDERATION**. You must also ensure that you can reach these hosts. Try pining them.

Example from */etc/contrail/contrail-federation-web/remote_metadata.xml*:

.. code-block:: xml

	<md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="http://google.contrail-idp.xlab.si/simplesaml/saml2/idp/metadata.php">
 		<md:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
    	<md:KeyDescriptor use="signing">
      		<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
        		<ds:X509Data>
        			Certdata
        		</ds:X509Data>
      		</ds:KeyInfo>
    	</md:KeyDescriptor>
		<md:KeyDescriptor use="encryption">
      		<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
        		<ds:X509Data>
                  Certdata
        		</ds:X509Data>
      		</ds:KeyInfo>
    	</md:KeyDescriptor>
		<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="http://google.contrail-idp.xlab.si/simplesaml/saml2/idp/SingleLogoutService.php"/>
    	<md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</md:NameIDFormat>
    	<md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="http://google.contrail-idp.xlab.si/simplesaml/saml2/idp/SSOService.php"/>
  	</md:IDPSSODescriptor>
  	<md:ContactPerson contactType="technical">
    	<md:GivenName>Contrail</md:GivenName>
    	<md:SurName>admin</md:SurName>
    	<md:EmailAddress>ales.cernivec@xlab.si</md:EmailAddress>
  	</md:ContactPerson>
	</md:EntityDescriptor>


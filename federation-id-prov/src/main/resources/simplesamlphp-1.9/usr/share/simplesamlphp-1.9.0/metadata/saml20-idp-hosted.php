<?php
/**
 * SAML 2.0 IdP configuration for simpleSAMLphp.
 *
 * See: https://rnd.feide.no/content/idp-hosted-metadata-reference
 */

$metadata['__DYNAMIC:1__'] = array(
	/*
	 * The hostname of the server (VHOST) that will use this SAML entity.
	 *
	 * Can be '__DEFAULT__', to use this entry by default.
	 */
	'host' => 'federation.contrail-idp.xlab.si',

	/* X.509 key and certificate. Relative to the cert directory. */
	'privatekey' => 'contrail-federation-idp.key',
	'certificate' => 'contrail-federation-idp.cert',

	/*
	 * Authentication source to use. Must be one that is configured in
	 * 'config/authsources.php'.
	 */
	'auth' => 'example-userpass',

	/* Uncomment the following to use the uri NameFormat on attributes. */
	/*
	'attributes.NameFormat' => 'urn:oasis:names:tc:SAML:2.0:attrname-format:uri',
	'authproc' => array(
		// Convert LDAP names to oids.
		100 => array('class' => 'core:AttributeMap', 'name2oid'),
	),
	*/

);

$metadata['__DYNAMIC:2__'] = array(
        /*
         * The hostname of the server (VHOST) that will use this SAML entity.
         *
         * Can be '__DEFAULT__', to use this entry by default.
         */
        'host' => 'google.contrail-idp.xlab.si',

        /* X.509 key and certificate. Relative to the cert directory. */
        'privatekey' => 'contrail-federation-idp.key',
        'certificate' => 'contrail-federation-idp.cert',

        /*
         * Authentication source to use. Must be one that is configured in
         * 'config/authsources.php'.
         */
        'auth' => 'google-contrail',

        /* Uncomment the following to use the uri NameFormat on attributes. */
        /*
        'attributes.NameFormat' => 'urn:oasis:names:tc:SAML:2.0:attrname-format:uri',
        'authproc' => array(
                // Convert LDAP names to oids.
                100 => array('class' => 'core:AttributeMap', 'name2oid'),
        ),
        */

);


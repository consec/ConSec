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
        'host' => 'multi.contrail-idp.contrail.eu',

        /* X.509 key and certificate. Relative to the cert directory. */
        'privatekey' => 'wildcard-contrail-idp.key',
        'certificate' => 'wildcard-contrail-idp.cert',

        /*
         * Authentication source to use. Must be one that is configured in
         * 'config/authsources.php'.
         */
        'auth' => 'contrail-multi',

);


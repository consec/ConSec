<?php
/**
 * SAML 1.1 remote IdP metadata for simpleSAMLphp.
 *
 * Remember to remove the IdPs you don't use from this file.
 *
 * See: https://rnd.feide.no/content/idp-remote-metadata-reference
 */

/*
$metadata['theproviderid-of-the-idp'] = array(
        'SingleSignOnService'  => 'https://idp.example.org/shibboleth-idp/SSO',
        'certFingerprint'      => 'c7279a9f28f11380509e072441e3dc55fb9ab864',
);
*/


$metadata['https://idp.testshib.org/idp/shibboleth'] = array(
        'name' => array(
                'en' => 'TestShibb',
        ),
        'acs.Bindings' => array(
            'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST',
            'urn:oasis:names:tc:SAML:1.0:profiles:browser-post',
         ),
        'description'          => 'TestShibb IdP',
        'SingleSignOnService'  => 'https://idp.testshib.org/idp/profile/Shibboleth/SSO',
        'certFingerprint'      => '0e82a794ce98b4c0e084fff4fc9514270cdb941a'
);

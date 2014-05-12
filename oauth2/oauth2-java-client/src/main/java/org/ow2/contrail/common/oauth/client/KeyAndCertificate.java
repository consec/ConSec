package org.ow2.contrail.common.oauth.client;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class KeyAndCertificate {
    private PrivateKey privateKey;
    private X509Certificate certificate;

    public KeyAndCertificate(PrivateKey privateKey, X509Certificate certificate) {
        this.privateKey = privateKey;
        this.certificate = certificate;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }
}

package org.ow2.contrail.common.oauth.client.utils;

import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMReader;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class CertUtils {

    public static KeyPair generateKeyPair(final String algorithm, final int keylen) throws NoSuchAlgorithmException {

        KeyPairGenerator kpGen = KeyPairGenerator.getInstance(algorithm);
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        kpGen.initialize(keylen, random);
        return kpGen.generateKeyPair();
    }

    public static PKCS10CertificationRequest createCSR(
            final KeyPair keyPair,
            final String subject,
            final String signatureAlgorithm) throws Exception {

        return new PKCS10CertificationRequest(signatureAlgorithm, new X500Principal(subject),
                keyPair.getPublic(), null, keyPair.getPrivate());
    }

    public static X509Certificate readCertificate(InputStreamReader isr) throws IOException {

        PEMReader pemReader = new PEMReader(isr);
        Object o = pemReader.readObject();
        if (o == null) {
            throw new IOException("Failed to read PEM object from input stream.");

        }
        return (X509Certificate) o;
    }
}

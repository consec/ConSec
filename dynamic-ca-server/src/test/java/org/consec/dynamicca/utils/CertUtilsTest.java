package org.consec.dynamicca.utils;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.consec.dynamicca.Utils;
import org.consec.dynamicca.jpa.EMF;
import org.consec.dynamicca.jpa.entities.Ca;
import org.consec.dynamicca.jpa.entities.Cert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.UUID;

import static org.junit.Assert.*;

public class CertUtilsTest {

    @Before
    public void setUp() throws Exception {
        EMF.init("testPersistenceUnit");
        Conf.getInstance().load(new File("src/test/resources/dynamic-ca-server.properties"));
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @After
    public void tearDown() throws Exception {
        EMF.close();
        Utils.dropTestDatabase();
    }

    @Test
    public void testCreateCaCert() throws Exception {
        EntityManager em = EMF.createEntityManager();
        CertUtils certUtils = new CertUtils(em);

        Ca ca1 = new Ca();
        ca1.setUid("ca1");
        Ca ca2 = new Ca();
        ca2.setUid("ca2");

        em.getTransaction().begin();
        em.persist(ca1);
        em.persist(ca2);
        em.getTransaction().commit();

        assertEquals(ca1.getSeqNum(), 1);
        assertEquals(ca2.getSeqNum(), 2);
        assertEquals(ca1.getCertSnCounter(), 1);
        assertEquals(ca2.getCertSnCounter(), 1);

        certUtils.createCACertificate(ca1);
        assertNotNull(ca1.getCertificate());
        assertNotNull(ca1.getPrivateKey());

        PEMReader pemReader = new PEMReader(new StringReader(ca1.getPrivateKey()));
        KeyPair keyPair = (KeyPair) pemReader.readObject();
        assertEquals(keyPair.getPrivate().getAlgorithm(), "RSA");

        pemReader = new PEMReader(new StringReader(ca1.getCertificate()));
        X509Certificate cert = (X509Certificate) pemReader.readObject();
        assertEquals(cert.getType(), "X.509");
        assertEquals(cert.getSerialNumber().intValue(), 1);
        assertEquals(cert.getIssuerDN().getName(),
                "E=test@consec.org,CN=ConSec Test CA,O=ConSec,L=Ljubljana,ST=Slovenia,C=SI");
        assertEquals(cert.getPublicKey().getAlgorithm(), "RSA");
        assertEquals(cert.getSigAlgName(), "SHA512WithRSAEncryption");

        String subjectDN = Conf.getInstance().getCACertDNTemplate()
                .replace("{UUID}", ca1.getUid());
        assertEquals(cert.getSubjectDN().getName(), subjectDN);

        // verify that this certificate was signed using the root-ca private key
        PEMReader pemReader1 = new PEMReader(new FileReader(Conf.getInstance().getRootCACertFile()));
        X509Certificate rootCACertificate = (X509Certificate) pemReader1.readObject();
        cert.verify(rootCACertificate.getPublicKey());
    }

    @Test
    public void testCreateUserCertificate() throws Exception {
        EntityManager em = EMF.createEntityManager();
        CertUtils certUtils = new CertUtils(em);

        Ca ca = new Ca();
        ca.setUid(UUID.randomUUID().toString());
        ca.setName("Test CA");

        em.getTransaction().begin();
        em.persist(ca);
        certUtils.createCACertificate(ca);
        em.getTransaction().commit();

        assertEquals(ca.getSeqNum(), 1);
        assertEquals(ca.getCertSnCounter(), 1);

        String userUuid = UUID.randomUUID().toString();

        Cert userCert = certUtils.createUserCertificate(userUuid, ca);

        // CA certificate
        PEMReader pemReader = new PEMReader(new StringReader(ca.getCertificate()));
        X509Certificate caCert = (X509Certificate) pemReader.readObject();

        // check certificate
        pemReader = new PEMReader(new StringReader(userCert.getCertificate()));
        X509Certificate x509Cert = (X509Certificate) pemReader.readObject();
        assertEquals(x509Cert.getType(), "X.509");
        assertEquals(x509Cert.getSerialNumber(), BigInteger.valueOf(1));
        assertEquals(x509Cert.getIssuerDN().getName(), caCert.getSubjectDN().getName());
        assertEquals(x509Cert.getPublicKey().getAlgorithm(), "RSA");
        assertEquals(x509Cert.getSigAlgName(), "SHA256WithRSAEncryption");

        String subjectDN = Conf.getInstance().getUserCertDNTemplate()
                .replace("{UUID}", userUuid);
        assertEquals(x509Cert.getSubjectDN().getName(), subjectDN);

        // verify that this certificate was signed using the ca's private key
        x509Cert.verify(caCert.getPublicKey());

        // check that cert counter was increased
        assertEquals(ca.getCertSnCounter(), 2);
    }

    @Test
    public void testCreateCRL() throws Exception {
        EntityManager em = EMF.createEntityManager();
        CertUtils certUtils = new CertUtils(em);

        // create CA
        Ca ca = new Ca();
        ca.setUid("myca");

        em.getTransaction().begin();
        em.persist(ca);
        certUtils.createCACertificate(ca);
        em.getTransaction().commit();

        // create CRL
        X509CRLHolder crlHolder = certUtils.createCRL(ca);

        // check CRL number
        ASN1Integer crlNumber = (ASN1Integer) crlHolder.getExtension(X509Extension.cRLNumber).getParsedValue();
        assertEquals(crlNumber.getValue().intValue(), 1);
        assertEquals(ca.getCrlCounter(), 2);

        // check issuer
        X509Certificate caCert = certUtils.getCaCert(ca);
        assertEquals(crlHolder.getIssuer().toString(), caCert.getSubjectDN().getName());

        assertTrue(crlHolder.isSignatureValid(
                new JcaContentVerifierProviderBuilder().setProvider("BC").build(caCert)));

        // create another CRL, check CRL number
        crlHolder = certUtils.createCRL(ca);
        crlNumber = (ASN1Integer) crlHolder.getExtension(X509Extension.cRLNumber).getParsedValue();
        assertEquals(crlNumber.getValue().intValue(), 2);
        assertEquals(ca.getCrlCounter(), 3);
    }
}

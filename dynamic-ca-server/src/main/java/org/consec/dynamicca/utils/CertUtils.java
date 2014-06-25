package org.consec.dynamicca.utils;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.consec.dynamicca.jpa.entities.Ca;
import org.consec.dynamicca.jpa.entities.Cert;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class CertUtils {
    private static final int CA_CERT_KEY_LENGTH = 2048;
    private static final int USER_CERT_KEY_LENGTH = 2048;

    private EntityManager em;

    public CertUtils(EntityManager em) {
        this.em = em;
    }

    public void createCACertificate(Ca ca) throws Exception {

        // read CA private key
        PrivateKey issuerPrivateKey = readPrivateKey(
                new File(Conf.getInstance().getRootCAPrivateKeyFile()),
                Conf.getInstance().getRootCAPrivateKeyPass());

        // read CA certificate
        PEMReader pemReader = new PEMReader(new FileReader(Conf.getInstance().getRootCACertFile()));
        X509Certificate issuerCert = (X509Certificate) pemReader.readObject();
        KeyPair issuerKeyPair = new KeyPair(issuerCert.getPublicKey(), issuerPrivateKey);
        String issuerName = issuerCert.getSubjectDN().getName();
        String issuerNameReversed = reverse(issuerName);

        KeyPair subjectKeyPair = generateKeyPair(CA_CERT_KEY_LENGTH);
        String subjectDN = Conf.getInstance().getCACertDNTemplate()
                .replace("{UUID}", ca.getUid());

        JcaContentSignerBuilder contetnSignerBuilder = new JcaContentSignerBuilder("SHA1withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME);
        ContentSigner contentSigner = contetnSignerBuilder.build(issuerPrivateKey);

        X500Name subjectX500Name = new X500Name(subjectDN);
        X500Name issuerX500Name = new X500Name(issuerNameReversed);

        Calendar startDate = new GregorianCalendar();
        Calendar expiryDate = (Calendar) startDate.clone();
        expiryDate.add(Calendar.DAY_OF_YEAR, Conf.getInstance().getCACertLifetimeDays());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerX500Name,
                BigInteger.valueOf(ca.getSeqNum()),
                startDate.getTime(),
                expiryDate.getTime(),
                subjectX500Name,
                subjectKeyPair.getPublic());

        certBuilder.addExtension(X509Extension.authorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(issuerKeyPair.getPublic()));

        certBuilder.addExtension(X509Extension.subjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(subjectKeyPair.getPublic()));

        certBuilder.addExtension(X509Extension.basicConstraints, true,
                new BasicConstraints(true));

        certBuilder.addExtension(X509Extension.keyUsage, true,
                new X509KeyUsage(
                        X509KeyUsage.cRLSign | X509KeyUsage.keyCertSign
                )
        );

        X509CertificateHolder certHolder = certBuilder.build(contentSigner);

        X509Certificate subjectCert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certHolder);

        String privateKeyPem = convertToPem(subjectKeyPair.getPrivate());
        ca.setPrivateKey(privateKeyPem);
        String certPem = convertToPem(subjectCert);
        ca.setCertificate(certPem);
    }

    public Cert createUserCertificate(String userUuid, Ca ca) throws Exception {

        PrivateKey caPrivateKey = getCaPrivateKey(ca);
        X509Certificate caCert = getCaCert(ca);

        KeyPair issuerKeyPair = new KeyPair(caCert.getPublicKey(), caPrivateKey);
        String issuerName = caCert.getSubjectDN().getName();

        int serialNumber;
        synchronized (CertUtils.class) {
            em.getTransaction().begin();
            serialNumber = ca.getCertSnCounter();
            ca.setCertSnCounter(serialNumber + 1);
            em.getTransaction().commit();
        }

        Calendar startDate = new GregorianCalendar();
        Calendar expiryDate = (Calendar) startDate.clone();
        expiryDate.add(Calendar.YEAR, 1);
        KeyPair keyPair = generateKeyPair(USER_CERT_KEY_LENGTH);
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        String subjectDN = Conf.getInstance().getUserCertDNTemplate()
                .replace("{UUID}", userUuid);
        String subjectDNReversed = reverse(subjectDN);
        X500Principal subjectPrincipal = new X500Principal(subjectDNReversed);
        String signatureAlgorithm = "SHA256withRSA";

        certGen.setSerialNumber(BigInteger.valueOf(serialNumber));
        certGen.setIssuerDN(caCert.getSubjectX500Principal());
        certGen.setNotBefore(startDate.getTime());
        certGen.setNotAfter(expiryDate.getTime());
        certGen.setSubjectDN(subjectPrincipal);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm(signatureAlgorithm);

        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(keyPair.getPublic()));

        X509Certificate x509Cert = certGen.generate(caPrivateKey, "BC");

        // create Cert object
        String privateKeyPem = convertToPem(keyPair.getPrivate());
        String certPem = convertToPem(x509Cert);
        Cert userCert = new Cert(x509Cert.getSerialNumber().intValue(), ca.getUid());
        userCert.setCa(ca);
        userCert.setPrivateKey(privateKeyPem);
        userCert.setCertificate(certPem);

        return userCert;
    }

    public String convertToPem(Object o) throws IOException {
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        pemWriter.writeObject(o);
        pemWriter.flush();
        pemWriter.close();
        return writer.toString();
    }

    public X509Certificate getCaCert(Ca ca) throws IOException {
        PEMReader pemReader = new PEMReader(new StringReader(ca.getCertificate()));
        return (X509Certificate) pemReader.readObject();
    }

    public PrivateKey getCaPrivateKey(Ca ca) throws IOException, NoSuchAlgorithmException {
        PEMReader pemReader = new PEMReader(new StringReader(ca.getPrivateKey()));
        KeyPair keyPair = (KeyPair) pemReader.readObject();
        return keyPair.getPrivate();
    }

    public PrivateKey readPrivateKey(File filePath, final String password) throws IOException {
        // read CA private key
        PEMReader pemReader = null;
        if (password != null) {
            PasswordFinder passwordFinder = new PasswordFinder() {
                @Override
                public char[] getPassword() {
                    return password.toCharArray();
                }
            };
            pemReader = new PEMReader(new FileReader(filePath), passwordFinder);
        }
        else {
            pemReader = new PEMReader(new FileReader(filePath));
        }
        KeyPair keyPair = (KeyPair) pemReader.readObject();
        return keyPair.getPrivate();
    }

    public X509CRLHolder createCRL(Ca ca) throws Exception {

        X509Certificate caCert = getCaCert(ca);
        PrivateKey caPrivateKey = getCaPrivateKey(ca);

        Date now = new Date();
        X509v2CRLBuilder crlGen = new X509v2CRLBuilder(new X500Name(caCert.getSubjectDN().getName()), now);

        Date nextUpdate = new Date(now.getTime() + 24 * 3600 * 1000); // Every day
        crlGen.setNextUpdate(nextUpdate);

        // get CRL number and increase counter
        int crlNumber;
        synchronized (CertUtils.class) {
            em.getTransaction().begin();
            crlNumber = ca.getCrlCounter();
            ca.setCrlCounter(crlNumber + 1);
            em.getTransaction().commit();
        }

        // set CRL number
        crlGen.addExtension(X509Extension.cRLNumber, false, new CRLNumber(BigInteger.valueOf(crlNumber)));

        // set authority key identifier
        crlGen.addExtension(X509Extension.authorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCert));

        // add revoked certificates
        TypedQuery<Cert> q = em.createNamedQuery("Cert.getRevokedCerts", Cert.class);
        q.setParameter("caUid", ca.getUid());
        List<Cert> certList = q.getResultList();
        for (Cert cert : certList) {
            BigInteger sn = BigInteger.valueOf(cert.getCertPK().getSn());
            crlGen.addCRLEntry(sn, cert.getRevocationDate(), CRLReason.unspecified);
        }

        // sign with CA private key
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(caPrivateKey);
        X509CRLHolder crlHolder = crlGen.build(contentSigner);

        return crlHolder;
    }

    public KeyPair generateKeyPair(final int keylen) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        keyPairGen.initialize(keylen, random);
        return keyPairGen.generateKeyPair();
    }

    private String reverse(String name) {
        String[] split = name.split(",");
        String nameReversed = "";
        for (int i=split.length - 1; i>=0; i--) {
            nameReversed += split[i];
            if (i>0) {
                nameReversed += ",";
            }
        }
        return nameReversed;
    }
}

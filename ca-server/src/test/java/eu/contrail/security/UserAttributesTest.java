/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.contrail.security;

import eu.contrail.security.servercommons.SAML;
import eu.contrail.security.servercommons.UserSAML;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.Test;
import org.ow2.contrail.federation.federationdb.jpa.entities.UGroup;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.security.auth.x500.X500Principal;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Iterator;

/**
 * @author ijj
 */
public class UserAttributesTest {

    //  @BeforeClass
    public static void setUpClass() throws Exception {

        Security.addProvider(new BouncyCastleProvider());
        PersistenceUtils pu = PersistenceUtils.getInstance();

        if (pu == null) {
            PersistenceUtils.createInstance("appPU"); // Ideally, should be a getinstance call which does
            // createInstance if no instance constructed yet

            // TODO - change name of persistence unit to read from config file

        }


    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void dummy() {
        System.err.append("dummy");
    }

    //  @Test
    public void testGetUserAttributes() throws Exception {

        System.out.println("get User Attributes");

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        UserSAML userSAML = new UserSAML();
        SAML saml = new SAML();


//    queryString = "SELECT u FROM User u WHERE u.username = :username AND u.password = :password";

        final String username = "contrailuser";
        final String password = "password";

        User user = null;


        try {

            user = userSAML.getUser(em, username, password);

        }
        catch (NoResultException ex) {

            throw new Exception(ex);

        }
        catch (NonUniqueResultException ex) {

            throw new Exception(ex);

        }


        Iterator<UGroup> iterGroup = user.getUGroupList().iterator();

        String xml = userSAML.getSAMLforUser(user, saml);

        System.err.println("\n\n\n");

        System.err.printf("Pretty Printer:\n%s", xml);

        final String oid = SecurityCommons.CONTRAIL_ATTRIBUTE_ASSERTION;
        final boolean critical = false;

        SecurityCommons sc = new SecurityCommons();

        X509Certificate prodCAcert =
                sc.getCertFromStream(new FileInputStream("src/test/resources/rootca-cert.pem"));
        PrivateKey prodCAkey = sc.readPrivateKey("src/test/resources/rootca-key.pem", null);


        BigInteger bigSerial = BigInteger.valueOf(System.currentTimeMillis());

        String issuerName;

        issuerName = prodCAcert.getSubjectDN().getName();
        issuerName = StringUtils.replace(issuerName, "\"", "");
        issuerName = sc.reverse(issuerName, ",");

        final String subjectName = String.format("CN=%s, CN=%s", user.getUserId(), user.getUsername());

        final X500Principal subjectPrincipal = new X500Principal(issuerName + "," + subjectName);

        X509Certificate cert = null;

        String uuid = user.getUuid();

        KeyPair kp = sc.generateKeyPair("RSA", 1024);

        cert = sc.createUserCertificateWithSAML(
                kp.getPublic(),
                subjectPrincipal,
                uuid,
                bigSerial,
                prodCAcert,
                prodCAkey,
                "SHA1withRSA", // TODO: This constant should be a parameter read at startup time
                730, 0, 0,
                oid, critical, xml);

        System.out.println("Certificate");

        sc.writeCertificate(System.out, cert);


        String assString = sc.getSAMLAssertion(cert);

        System.out.printf("\n\nAssertion = \n%s\n", assString);

    }

}

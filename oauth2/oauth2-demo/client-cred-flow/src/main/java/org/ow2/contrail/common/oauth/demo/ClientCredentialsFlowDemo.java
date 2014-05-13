package org.ow2.contrail.common.oauth.demo;

import org.bouncycastle.openssl.PEMWriter;
import org.ow2.contrail.common.oauth.client.AccessToken;
import org.ow2.contrail.common.oauth.client.CCFlowClient;
import org.ow2.contrail.common.oauth.client.CertRetriever;
import org.ow2.contrail.common.oauth.client.KeyAndCertificate;
import org.ow2.contrail.common.oauth.demo.utils.Conf;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;

public class ClientCredentialsFlowDemo {
    private static final String CONF_FILE = "/etc/contrail/oauth2-client-cred-flow-demo/oauth2-client-cred-flow-demo.conf";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage:\n" +
                    "  ClientCredentialsFlowDemo getToken <userUUID>\n" +
                    "  ClientCredentialsFlowDemo getCert <accessToken>");
            return;
        }

        Conf.getInstance().load(new File(CONF_FILE));

        String commmand = args[0];
        if (commmand.equals("getToken")) {
            String resourceOwner = args[1];
            System.out.println(String.format(
                    "Requesting OAuth access token from the Authorisation Server %s on behalf of the user %s.",
                    Conf.getInstance().getASAccessTokenUri(), resourceOwner));
            try {
                AccessToken accessToken = getToken(resourceOwner, null);
                System.out.println("Received access token: " + accessToken.getValue());
            }
            catch (Exception e) {
                System.out.println("Failed to obtain an access token: " + e.getMessage());
            }
        }
        else if (commmand.equals("getCert")) {
            String accessToken = args[1];
            System.out.println(String.format(
                    "Requesting delegated user certificate from the CA server %s using access token %s.",
                    Conf.getInstance().getCAUserCertUri(), accessToken));
            try {
                KeyAndCertificate keyAndCertificate = getCert(accessToken);
                System.out.println("Received user certificate.");
                System.out.println("Private key:");
                System.out.println(convertToPem(keyAndCertificate.getPrivateKey()));
                System.out.println("Certificate:");
                System.out.println(convertToPem(keyAndCertificate.getCertificate()));
            }
            catch (Exception e) {
                System.out.println("Failed to obtain user certificate: " + e.getMessage());
            }
        }
        else {
            System.out.println("Invalid command.");
        }
    }

    public static AccessToken getToken(String resourceOwner, String scope) throws Exception {
        URI tokenEndpointUri = new URI(Conf.getInstance().getASAccessTokenUri());
        CCFlowClient ccFlowClient = new CCFlowClient(tokenEndpointUri,
                Conf.getInstance().getClientKeystoreFile(), Conf.getInstance().getClientKeystorePass(),
                Conf.getInstance().getClientTruststoreFile(), Conf.getInstance().getClientTruststorePass());
        ccFlowClient.setClientId(Conf.getInstance().getClientId());
        ccFlowClient.setClientSecret(Conf.getInstance().getClientSecret());

        return ccFlowClient.requestAccessToken(resourceOwner, scope);
    }

    public static KeyAndCertificate getCert(String accessToken) throws Exception {
        URI userCertEndpointUri = new URI(Conf.getInstance().getCAUserCertUri());
        CertRetriever certRetriever = new CertRetriever(userCertEndpointUri,
                Conf.getInstance().getClientKeystoreFile(), Conf.getInstance().getClientKeystorePass(),
                Conf.getInstance().getClientTruststoreFile(), Conf.getInstance().getClientTruststorePass());

        return certRetriever.retrieveCert(accessToken);
    }

    private static String convertToPem(Object o) throws IOException {
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        pemWriter.writeObject(o);
        pemWriter.flush();
        pemWriter.close();
        return writer.toString();
    }
}

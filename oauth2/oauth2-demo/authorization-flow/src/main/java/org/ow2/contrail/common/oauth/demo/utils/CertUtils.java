package org.ow2.contrail.common.oauth.demo.utils;

import org.bouncycastle.openssl.PEMWriter;
import org.ow2.contrail.common.oauth.client.CertRetriever;
import org.ow2.contrail.common.oauth.client.KeyAndCertificate;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;

public class CertUtils {

    public static KeyAndCertificate retrieveCert(String accessToken) throws Exception {
        URI endpointUri = new URI(Conf.getInstance().getCAUserCertUri());
        CertRetriever certRetriever = new CertRetriever(endpointUri,
                Conf.getInstance().getClientKeystoreFile(), Conf.getInstance().getClientKeystorePass(),
                Conf.getInstance().getClientTruststoreFile(), Conf.getInstance().getClientTruststorePass());
        return certRetriever.retrieveCert(accessToken);
    }

    public static String convertToPem(Object o) throws IOException {
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        pemWriter.writeObject(o);
        pemWriter.flush();
        pemWriter.close();
        return writer.toString();
    }
}

package org.consec.oauth2.authzserver.saml;

import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.parse.BasicParserPool;

import java.io.File;

public class SAMLMetadata {

    private MetadataProvider metadataProvider = null;
    private String idpEntityId;
    private String spEntityId;

    static {
        try {
            DefaultBootstrap.bootstrap();
        }
        catch (ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    public SAMLMetadata(File metadataFile, String idpEntityId, String spEntityId) throws MetadataProviderException {
        this.idpEntityId = idpEntityId;
        this.spEntityId = spEntityId;
        metadataProvider = createMetadataProvider(metadataFile);
    }

    public MetadataProvider getMetadataProvider() {
        return metadataProvider;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public String getSpEntityId() {
        return spEntityId;
    }

    private MetadataProvider createMetadataProvider(File metadataFile) throws MetadataProviderException {
        try {
            FilesystemMetadataProvider provider = new FilesystemMetadataProvider(metadataFile);
            provider.setParserPool(new BasicParserPool());
            provider.initialize();
            return provider;
        }
        catch (MetadataProviderException e) {
            throw new MetadataProviderException(String.format("Failed to load SAML metadata file %s: %s",
                    metadataFile.getAbsolutePath(), e.getMessage()), e);
        }
    }
}

package org.consec.oauth2.authzserver.saml;

import org.opensaml.common.binding.BasicEndpointSelector;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.metadata.*;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;

import javax.xml.namespace.QName;
import java.util.List;

public class OpenSAMLUtils {

    public static void initializeLocalEntity(SAMLMessageContext context, EntityDescriptor entity,
                                             RoleDescriptor role, QName endpointType) {
        context.setLocalEntityId(entity.getEntityID());
        context.setLocalEntityMetadata(entity);
        context.setLocalEntityRole(endpointType);
        context.setLocalEntityRoleMetadata(role);
        context.setOutboundMessageIssuer(entity.getEntityID());
    }

    public static void initializePeerEntity(SAMLMessageContext context, EntityDescriptor entity,
                                            RoleDescriptor role, QName endpointType, String binding) {
        context.setPeerEntityId(entity.getEntityID());
        context.setPeerEntityMetadata(entity);
        context.setPeerEntityRole(endpointType);
        context.setPeerEntityRoleMetadata(role);

        BasicEndpointSelector selector = new BasicEndpointSelector();
        selector.setEntityMetadata(entity);
        selector.setEndpointType(endpointType);
        selector.setEntityRoleMetadata(role);
        selector.getSupportedIssuerBindings().add(binding);
        context.setPeerEntityEndpoint(selector.selectEndpoint());
    }

    public static String getLogoutBinding(IDPSSODescriptor idp, SPSSODescriptor sp) throws MetadataProviderException {

        List<SingleLogoutService> logoutServices = idp.getSingleLogoutServices();
        if (logoutServices.size() == 0) {
            throw new MetadataProviderException("IdP does not contain any SingleLogout endpoints.");
        }

        String binding = null;

        idp:
        for (SingleLogoutService idpService : logoutServices) {
            for (SingleLogoutService spService : sp.getSingleLogoutServices()) {
                if (idpService.getBinding().equals(spService.getBinding())) {
                    binding = idpService.getBinding();
                    break idp;
                }
            }
        }

        if (binding == null) {
            binding = idp.getSingleLogoutServices().iterator().next().getBinding();
        }

        return binding;
    }

    public static SingleLogoutService getLogoutServiceForBinding(SSODescriptor descriptor, String binding) throws MetadataProviderException {
        List<SingleLogoutService> services = descriptor.getSingleLogoutServices();
        for (SingleLogoutService service : services) {
            if (binding.equals(service.getBinding())) {
                return service;
            }
        }

        throw new MetadataProviderException(String.format(
                "Binding %s is not supported for this IdP.", binding));
    }
}

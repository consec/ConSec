package org.ow2.contrail.federation.federationapi.utils;

import org.consec.federationdb.model.*;

public class RestUriBuilder {
    private static String idpUriFormat = "/idps/%s";
    private static String userUriFormat = "/users/%s";
    private static String attributeUriFormat = "/attributes/%s";
    private static String userAttrUriFormat = "/users/%s/attributes/%d";
    private static String roleUriFormat = "/roles/%d";
    private static String groupUriFormat = "/groups/%d";

    public static String getIdpUri(IdentityProvider idp) {
        return String.format(idpUriFormat, idp.getIdpId());
    }

    public static String getUserUri(User user) {
        return String.format(userUriFormat, user.getUserId());
    }

    public static String getAttributeUri(Attribute attribute) {
        return String.format(attributeUriFormat, attribute.getAttributeId());
    }

    public static String getRoleUri(Role role) {
        return String.format(roleUriFormat, role.getRoleId());
    }

    public static String getGroupUri(Group group) {
        return String.format(groupUriFormat, group.getGroupId());
    }

    public static String getUserAttrUri(UserHasAttribute attr) {
        return String.format(userAttrUriFormat, attr.getUser().getUserId(),
                attr.getUserHasAttributePK().getAttributeId());
    }
}


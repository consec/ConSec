package org.ow2.contrail.federation.federationapi.authorization;

import org.consec.federationdb.model.Group;
import org.consec.federationdb.model.Role;
import org.consec.federationdb.model.User;
import org.ow2.contrail.federation.herasafauthorizer.Rule;
import org.ow2.contrail.provider.cnr_pep_java.Attribute;
import org.ow2.contrail.provider.cnr_pep_java.PEP_callout;
import org.ow2.contrail.provider.cnr_pep_java.XACMLType;

import java.util.ArrayList;
import java.util.List;

public class SoapXACMLAuthorizer extends Authorizer {
    private String pdp_endpoint;

    public SoapXACMLAuthorizer(String pdpURL) {
        pdp_endpoint = pdpURL;
    }

    public boolean isAuthorized(User user, String resourceURI, Action action) {
        List<Attribute> accessRequest = new ArrayList<Attribute>();
        String issuer = "Contrail";

        // subject: UUID
        Attribute subjectAttr = new Attribute(
                "urn:contrail:names:federation:subject:uuid",
                XACMLType.STRING,
                user.getUserId(),
                issuer,
                Attribute.SUBJECT);
        accessRequest.add(subjectAttr);

        // subject: group
        if (user.getGroupList() != null) {
            for (Group group : user.getGroupList()) {
                subjectAttr = new Attribute(
                        "urn:contrail:names:federation:subject:group",
                        XACMLType.STRING,
                        group.getName(),
                        issuer,
                        Attribute.SUBJECT);
                accessRequest.add(subjectAttr);
            }
        }

        // subject: role
        if (user.getRoleList() != null) {
            for (Role role : user.getRoleList()) {
                subjectAttr = new Attribute(
                        "urn:contrail:names:federation:subject:role",
                        XACMLType.STRING,
                        role.getName(),
                        issuer,
                        Attribute.SUBJECT);
                accessRequest.add(subjectAttr);
            }
        }

        // TODO: add 'urn:contrail:names:federation:subject:provider-id' attribute for CloudAdministrator

        // resource
        Attribute resourceAttr = new Attribute(
                "urn:oasis:names:tc:xacml:1.0:resource:resource-id",
                XACMLType.STRING,
                resourceURI,
                issuer,
                Attribute.RESOURCE);
        accessRequest.add(resourceAttr);

        // action
        Attribute actionAttr = new Attribute(
                "urn:contrail:federation:action:id",
                XACMLType.STRING,
                action.toString(),
                issuer,
                Attribute.ACTION);
        accessRequest.add(actionAttr);

        PEP_callout pep_callout = new PEP_callout(pdp_endpoint);

        return pep_callout.isPermit(accessRequest);
    }

    @Override
    public Rule getRule(String ruleId) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Rule> getRules(User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Rule> getRules(Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String deployRule(User user, Rule rule) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public String deployRule(Group group, Rule rule) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateRule(String ruleId, Rule newRule) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRule(String ruleId) throws Exception {
        throw new UnsupportedOperationException();
    }
}

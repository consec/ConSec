package org.ow2.contrail.federation.federationapi.authorization;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.Group;
import org.consec.federationdb.model.Role;
import org.consec.federationdb.model.User;
import org.ow2.contrail.federation.herasafauthorizer.*;

import java.util.List;

public class HerasafAuthorizer extends Authorizer {
    private static Logger log = Logger.getLogger(HerasafAuthorizer.class);
    private HerasafXACMLAuthorizer authorizer;

    public HerasafAuthorizer(HerasafXACMLAuthorizer authorizer) throws Exception {
        this.authorizer = authorizer;
    }

    @Override
    public boolean isAuthorized(User user, String resourceUri, Action action) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("authorize() started: user=%s (UUID=%s), resource=%s, action=%s) started.",
                    user.getUsername(), user.getUserId(), resourceUri, action));
        }

        AuthSubject authSubject = new AuthSubject();

        // subject - user
        authSubject.addSubject(new Subject(Subject.Type.USER, user.getUserId()));

        // subject - groups
        if (user.getGroupList() != null) {
            for (Group uGroup : user.getGroupList()) {
                authSubject.addSubject(new Subject(Subject.Type.GROUP, uGroup.getName()));
            }
        }

        // subject - roles
        if (user.getRoleList() != null) {
            for (Role uRole : user.getRoleList()) {
                authSubject.addSubject(new Subject(Subject.Type.ROLE, uRole.getName()));
            }
        }

        if (log.isTraceEnabled()) {
            log.trace(String.format(
                    "Calling herasaf-authorizer isAuthorized(subject=%s, resourceUri=%s, action=%s)",
                    authSubject, resourceUri, action));
        }
        boolean isAuthorized = authorizer.isAuthorized(authSubject, resourceUri, action.name());
        log.trace("isAuthorized: " + isAuthorized);

        return isAuthorized;
    }

    @Override
    public Rule getRule(String ruleId) throws Exception {
        return authorizer.getRule(ruleId);
    }

    @Override
    public List<Rule> getRules(User user) {
        Subject subject = new Subject(Subject.Type.USER, user.getUserId());
        Policy policy = authorizer.getPolicy(subject);
        return policy.getRules();
    }

    @Override
    public List<Rule> getRules(Group group) {
        Subject subject = new Subject(Subject.Type.GROUP, group.getName());
        Policy policy = authorizer.getPolicy(subject);
        return policy.getRules();
    }

    @Override
    public String deployRule(User user, Rule rule) throws Exception {
        Subject subject = new Subject(Subject.Type.USER, user.getUserId());
        Policy policy = authorizer.getPolicy(subject);
        String ruleId = policy.addRule(rule);
        authorizer.redeployPolicy(policy);
        return ruleId;
    }

    @Override
    public String deployRule(Group uGroup, Rule rule) throws Exception {
        Subject subject = new Subject(Subject.Type.GROUP, uGroup.getName());
        Policy policy = authorizer.getPolicy(subject);
        String ruleId = policy.addRule(rule);
        authorizer.redeployPolicy(policy);
        return ruleId;
    }

    @Override
    public void updateRule(String ruleId, Rule newRule) throws Exception {
        Policy policy = authorizer.getPolicy(ruleId);
        policy.updateRule(ruleId, newRule);
        authorizer.redeployPolicy(policy);
    }

    @Override
    public void removeRule(String ruleId) throws Exception {
        Policy policy = authorizer.getPolicy(ruleId);
        policy.removeRule(ruleId);
        authorizer.redeployPolicy(policy);
    }
}

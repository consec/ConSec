package org.ow2.contrail.federation.federationapi.authorization;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.Group;
import org.consec.federationdb.model.User;
import org.consec.federationdb.utils.EMF;
import org.ow2.contrail.federation.federationapi.exceptions.AuthorizationException;
import org.ow2.contrail.federation.herasafauthorizer.Rule;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Authorizer {
    private static Logger log = Logger.getLogger(Authorizer.class);

    public abstract boolean isAuthorized(User user, String resourceURI, Action action);

    public boolean isAuthorized(HttpServletRequest request) throws AuthorizationException {
        User user = authenticate(request);
        String resourceURI = request.getPathInfo();

        // action
        String method = request.getMethod();
        Action action;
        if (method.equals("GET")) {
            action = Action.READ;
        }
        else if (method.equals("POST")) {
            action = Action.WRITE;
        }
        else if (method.equals("PUT")) {
            action = Action.WRITE;
        }
        else if (method.equals("DELETE")) {
            action = Action.WRITE;
        }
        else {
            throw new AuthorizationException("Invalid HTTP method: " + method);
        }

        return isAuthorized(user, resourceURI, action);
    }

    public User authenticate(HttpServletRequest request) throws AuthorizationException {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null) {
            throw new AuthorizationException("The client certificate was not provided.");
        }

        String uuid = null;
        try {
            Principal principal = certs[0].getSubjectDN();
            String dn = principal.getName();
            Pattern dnPattern = Pattern.compile("CN=([\\w-]+)");
            Matcher m = dnPattern.matcher(dn);
            if (m.find()) {
                uuid = m.group(1);
            }
            else {
                throw new AuthorizationException("Invalid client certificate distinguished name.");
            }
        }
        catch (Exception e) {
            throw new AuthorizationException("Invalid client certificate.");
        }

        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, uuid);
            if (user == null) {
                throw new AuthorizationException(
                        String.format("User with UUID '%s' is not registered in the federation database.", uuid));
            }

            return user;
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    public abstract Rule getRule(String ruleId) throws Exception;

    public abstract List<Rule> getRules(User user);

    public abstract List<Rule> getRules(Group group);

    public abstract String deployRule(User user, Rule rule) throws Exception;

    public abstract String deployRule(Group group, Rule rule) throws Exception;

    public abstract void updateRule(String ruleId, Rule newRule) throws Exception;

    public abstract void removeRule(String ruleId) throws Exception;
}

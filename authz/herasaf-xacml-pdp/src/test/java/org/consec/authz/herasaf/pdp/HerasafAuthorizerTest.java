package org.consec.authz.herasaf.pdp;

import org.apache.log4j.Logger;
import org.consec.authz.herasaf.pdp.core.*;
import org.herasaf.xacml.core.context.impl.DecisionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class HerasafAuthorizerTest {
    private static Logger log = Logger.getLogger(HerasafAuthorizerTest.class);
    private HerasafXACMLEngine authorizer;

    @org.junit.Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        log.trace(String.format("Using folder %s as persistent repository.", tempFolder.getRoot().getAbsolutePath()));
        FileBasedPolicyRepository repository = new FileBasedPolicyRepository(tempFolder.getRoot());
        authorizer = new HerasafXACMLEngine(repository);
    }

    @Test
    public void testUserRules() throws Exception {
        log.trace("testUserRules() started.");

        Subject user = new Subject(Subject.Type.USER, "68bcb06e-446f-43a6-9a3a-d9e915bc8480");
        AuthSubject authSubject = new AuthSubject();
        authSubject.addSubject(user);

        // deploy some auth rules for the user
        Policy policy = authorizer.getPolicy(user);
        List<Rule> rules = policy.getRules();
        assertEquals(rules.size(), 0);

        // rule 1
        Rule rule1 = createRule(
                user,
                new String[]{"/users/john/"},
                new String[]{Action.READ},
                "Rule 1 for user");
        String rule1Id = policy.addRule(rule1);

        // rule 2
        Rule rule2 = createRule(
                user,
                new String[]{"/users/john/docs/"},
                new String[]{Action.READ, Action.WRITE},
                "Rule 2 for user");
        String rule2Id = policy.addRule(rule2);

        // rule 3
        Rule rule3 = createRule(
                user,
                new String[]{"/users/john/docs/personal/"},
                new String[]{},
                "Rule 3 for user");
        String rule3Id = policy.addRule(rule3);

        authorizer.redeployPolicy(policy);

        // check new rules
        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/lucy/abcd", Action.READ), DecisionType.NOT_APPLICABLE);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/x", Action.READ), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/x", Action.WRITE), DecisionType.NOT_APPLICABLE);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/x", Action.READ), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/x", Action.WRITE), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/personal/x", Action.READ), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/personal/x", Action.WRITE), DecisionType.PERMIT);


        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john", Action.READ), DecisionType.PERMIT);
        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/", Action.READ), DecisionType.PERMIT);
        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/x", Action.READ), DecisionType.PERMIT);
        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/x/", Action.READ), DecisionType.PERMIT);
        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/x/y", Action.READ), DecisionType.PERMIT);


        // update auth rule 3
        String[] actions = new String[]{Action.READ, Action.WRITE};
        rule3.setActions(Arrays.asList(actions));
        policy.updateRule(rule3Id, rule3);
        authorizer.redeployPolicy(policy);

        // check updated rule
        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/personal/x", Action.READ), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/personal/x", Action.WRITE), DecisionType.PERMIT);

        // delete auth rule 2
        policy.removeRule(rule2Id);
        authorizer.redeployPolicy(policy);

        // check if rule was deleted
        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/x", Action.READ), DecisionType.PERMIT); // inherited from /users/john/

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/x", Action.WRITE), DecisionType.NOT_APPLICABLE);

        log.trace("testUserRules() finished successfully.");
    }

    @Test
    public void testUserWithGroupsRules() throws Exception {
        log.trace("testUserWithGroupsRules() started.");

        Subject user = new Subject(Subject.Type.USER, UUID.randomUUID().toString());
        Subject group1 = new Subject(Subject.Type.GROUP, UUID.randomUUID().toString());
        Subject group2 = new Subject(Subject.Type.GROUP, UUID.randomUUID().toString());
        AuthSubject authSubject = new AuthSubject();
        authSubject.addSubject(new Subject[]{user, group1, group2});

        // deploy some auth rules for the user
        Policy userPolicy = authorizer.getPolicy(user);
        List<Rule> rules = userPolicy.getRules();
        assertEquals(rules.size(), 0);

        String[] resourceURIs;
        Action[] actions;
        Date now = new Date();

        // rule 1 for user
        Rule ruleU1 = createRule(
                user,
                new String[]{"/users/john/"},
                new String[]{Action.READ},
                "Rule 1 for user");
        userPolicy.addRule(ruleU1);

        // rule 2 for user
        Rule ruleU2 = createRule(
                user,
                new String[]{"/users/john/docs/"},
                new String[]{Action.READ, Action.WRITE},
                "Rule 2 for user");
        userPolicy.addRule(ruleU2);

        // rule 3 for user
        Rule ruleU3 = createRule(
                user,
                new String[]{"/users/john/docs/personal/"},
                new String[]{},
                "Rule 3 for user");
        userPolicy.addRule(ruleU3);

        authorizer.redeployPolicy(userPolicy);


        // deploy some rules for group 1
        Policy group1Policy = authorizer.getPolicy(group1);

        // rule 1 for group 1
        Rule ruleG11 = createRule(
                group1,
                new String[]{"/shared/photos/summer2012/"},
                new String[]{Action.READ},
                "Rule 1 for group 1");
        group1Policy.addRule(ruleG11);

        // Rule 2 for group 1
        Rule ruleG12 = createRule(
                group1,
                new String[]{"/users/john/docs/"},
                new String[]{Action.READ},
                "Rule 2 for group 1");
        group1Policy.addRule(ruleG12);

        // Rule 3 for group 1
        Rule ruleG13 = createRule(
                group1,
                new String[]{"/users/john/docs/personal/"},
                new String[]{Action.READ},
                "Rule 3 for group 1");
        group1Policy.addRule(ruleG13);

        authorizer.redeployPolicy(group1Policy);


        // deploy some rules for group 2
        Policy group2Policy = authorizer.getPolicy(group2);

        // rule 1 for group 2
        Rule ruleG21 = createRule(
                group2,
                new String[]{"/shared/music/"},
                new String[]{Action.READ, Action.WRITE},
                "Rule 1 for group 2");
        group2Policy.addRule(ruleG21);

        // Rule 2 for group 2
        Rule ruleG22 = createRule(
                group2,
                new String[]{"/users/john/docs/tutorials/"},
                new String[]{Action.READ},
                "Rule 2 for group 2");
        group2Policy.addRule(ruleG22);

        authorizer.redeployPolicy(group2Policy);


        // check rule evaluation
        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/x", Action.READ), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/x", Action.WRITE), DecisionType.NOT_APPLICABLE);


        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/x", Action.READ), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/x", Action.WRITE), DecisionType.PERMIT);


        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/personal/x", Action.READ), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/personal/x", Action.WRITE), DecisionType.PERMIT);


        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/shared/photos/summer2012/x", Action.READ), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/shared/photos/summer2012/x", Action.WRITE),
                DecisionType.NOT_APPLICABLE);


        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/shared/music/x", Action.READ), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/shared/music/x", Action.WRITE), DecisionType.PERMIT);


        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/tutorials/x", Action.READ), DecisionType.PERMIT);

        assertEquals(
                authorizer.evaluateAccessRequest(authSubject, "/users/john/docs/tutorials/x", Action.WRITE), DecisionType.PERMIT);

        log.trace("testUserWithGroupsRules() finished successfully.");
    }

    @Test
    public void testInheritance() throws Exception {
        log.trace("testInheritance() started.");

        Subject user = new Subject(Subject.Type.USER, UUID.randomUUID().toString());
        AuthSubject authSubject = new AuthSubject();
        authSubject.addSubject(user);

        // deploy some auth rules for the user
        Policy policy = authorizer.getPolicy(user);
        List<Rule> rules = policy.getRules();
        assertEquals(rules.size(), 0);

        // rule 1
        Rule rule1 = createRule(
                user,
                new String[]{"/a/"},
                new String[]{Action.READ});
        policy.addRule(rule1);

        // rule 2
        Rule rule2 = createRule(
                user,
                new String[]{"/a/b/"},
                new String[]{Action.WRITE});
        policy.addRule(rule2);

        authorizer.redeployPolicy(policy);

        // check rules
        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/aaa", Action.READ), DecisionType.NOT_APPLICABLE);
        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/aaa", Action.WRITE), DecisionType.NOT_APPLICABLE);

        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/a/", Action.READ), DecisionType.PERMIT);
        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/a/", Action.WRITE), DecisionType.NOT_APPLICABLE);

        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/a/x", Action.READ), DecisionType.PERMIT);
        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/a/x", Action.WRITE), DecisionType.NOT_APPLICABLE);

        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/a/b", Action.READ), DecisionType.PERMIT);
        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/a/b", Action.WRITE), DecisionType.PERMIT);

        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/a/b/x", Action.READ), DecisionType.PERMIT);
        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/a/b/x", Action.WRITE), DecisionType.PERMIT);

        log.trace("testInheritance() finished successfully.");
    }

    @Test
    public void testRuleWithDateConstraint() {
        log.trace("testRuleWithDateConstraint() started.");

        Subject user = new Subject(Subject.Type.USER, UUID.randomUUID().toString());
        AuthSubject authSubject = new AuthSubject();
        authSubject.addSubject(user);

        // deploy some auth rules for the user
        Policy policy = authorizer.getPolicy(user);
        List<Rule> rules = policy.getRules();
        assertEquals(rules.size(), 0);

        // rule 1
        Rule rule1 = createRule(
                user,
                new String[]{"/a/"},
                new String[]{Action.READ});
        Date now = new Date();
        rule1.setStartTime(new Date(now.getTime()));
        rule1.setEndTime(new Date(now.getTime() + 3600 * 1000));
        policy.addRule(rule1);

        // rule 2
        Rule rule2 = createRule(
                user,
                new String[]{"/b/"},
                new String[]{Action.READ});
        rule2.setStartTime(new Date(now.getTime() + 60 * 1000));
        rule2.setEndTime(new Date(now.getTime() + 180 * 1000));
        policy.addRule(rule2);

        authorizer.redeployPolicy(policy);

        // check rules
        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/a/", Action.READ), DecisionType.PERMIT);
        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/a/", Action.WRITE), DecisionType.NOT_APPLICABLE);

        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/b/", Action.READ), DecisionType.NOT_APPLICABLE);
        assertEquals(authorizer.evaluateAccessRequest(authSubject, "/b/", Action.WRITE), DecisionType.NOT_APPLICABLE);

        log.trace("testRuleWithDateConstraint() finished successfully.");
    }

    private Rule createRule(Subject subject, String[] resourceURIs, String[] actions) {
        return createRule(subject, resourceURIs, actions, null);
    }

    private Rule createRule(Subject subject, String[] resourceURIs, String[] actions, String description) {
        Rule rule = new Rule();
        rule.setSubject(subject);
        rule.setResourceURIs(Arrays.asList(resourceURIs));
        rule.setActions(Arrays.asList(actions));
        rule.setDescription(description);
        return rule;
    }

    @After
    public void tearDown() throws Exception {
    }
}

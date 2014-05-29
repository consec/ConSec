package org.consec.authz.herasaf.pdp;

import org.apache.log4j.Logger;
import org.herasaf.xacml.core.PolicyRepositoryException;
import org.herasaf.xacml.core.SyntaxException;
import org.herasaf.xacml.core.WritingException;
import org.herasaf.xacml.core.api.PDP;
import org.herasaf.xacml.core.api.PolicyRetrievalPoint;
import org.herasaf.xacml.core.api.UnorderedPolicyRepository;
import org.herasaf.xacml.core.combiningAlgorithm.policy.PolicyCombiningAlgorithm;
import org.herasaf.xacml.core.combiningAlgorithm.policy.impl.PolicyPermitOverridesAlgorithm;
import org.herasaf.xacml.core.context.RequestCtx;
import org.herasaf.xacml.core.context.RequestCtxFactory;
import org.herasaf.xacml.core.context.ResponseCtx;
import org.herasaf.xacml.core.context.impl.DecisionType;
import org.herasaf.xacml.core.context.impl.ResponseType;
import org.herasaf.xacml.core.context.impl.ResultType;
import org.herasaf.xacml.core.dataTypeAttribute.impl.DateTimeDataTypeAttribute;
import org.herasaf.xacml.core.dataTypeAttribute.impl.StringDataTypeAttribute;
import org.herasaf.xacml.core.policy.Evaluatable;
import org.herasaf.xacml.core.policy.PolicyMarshaller;
import org.herasaf.xacml.core.policy.impl.*;
import org.herasaf.xacml.core.simplePDP.SimplePDPFactory;

import java.io.File;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class HerasafXACMLAuthorizer {
    private static Logger log = Logger.getLogger(HerasafXACMLAuthorizer.class);
    private static String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private PDP pdp;
    private UnorderedPolicyRepository policyRepository;
    private PolicyRetrievalPoint prp;

    public HerasafXACMLAuthorizer(UnorderedPolicyRepository policyRepository) throws Exception {
        log.trace("Initializing HerasafXACMLAuthorizer.");

        this.policyRepository = policyRepository;
        if (policyRepository instanceof PolicyRetrievalPoint) {
            this.prp = (PolicyRetrievalPoint) policyRepository;
        }
        else {
            throw new Exception("Invalid policy repository: does not implement PolicyRetrievalPoint interface.");
        }

        PolicyCombiningAlgorithm rootCombiningAlgorithm = new PolicyPermitOverridesAlgorithm();
        pdp = SimplePDPFactory.getSimplePDP(rootCombiningAlgorithm, prp);

        log.trace("HerasafXACMLAuthorizer initialized successfully.");
    }

    public HerasafXACMLAuthorizer(UnorderedPolicyRepository policyRepository,
                                  List<String> policyFilePaths) throws Exception {
        this(policyRepository);
        for (String policyFilePath : policyFilePaths) {
            deployPolicyFile(new File(policyFilePath));
        }
    }

    public void deployPolicyFile(File policyFile) throws SyntaxException {
        log.trace("Deploying policy file " + policyFile);
        Evaluatable evaluatable = PolicyMarshaller.unmarshal(policyFile);
        if (!isDeployed(evaluatable)) {
            policyRepository.deploy(evaluatable);
            log.trace("Policy was deployed successfully.");
        }
        else {
            log.trace("Policy is already deployed");
        }
    }

    public boolean isAuthorized(AuthSubject authSubject, String resourceURI, String action) {

        // subject
        RequestSubject reqSubject = new RequestSubject();
        for (Subject subject : authSubject.getSubjectList()) {
            reqSubject.addSubjectAttr(subject.getAttributeId(), new StringDataTypeAttribute(), subject.getId());
        }

        // resource
        RequestResource reqResource = new RequestResource();
        reqResource.addResourceAttr(Consts.RESOURCE_ID_ATTR,
                new StringDataTypeAttribute(),
                resourceURI);

        // action
        RequestAction reqAction = new RequestAction(Consts.ACTION_NAME_ATTR,
                new StringDataTypeAttribute(),
                action.toString());

        // environment
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(timeZone);
        String currentDate = sdf.format(new Date());
        RequestEnvironment reqEnvironment = new RequestEnvironment(
                Consts.ENVIRONMENT_CURRENT_TIME_ATTR,
                new DateTimeDataTypeAttribute(),
                currentDate);

        RequestCtx request = RequestCtxFactory.create(reqSubject, reqResource, reqAction, reqEnvironment);

        // dump XACML request
        if (log.isTraceEnabled()) {
            StringWriter writer = new StringWriter();
            try {
                request.marshal(writer);
                log.trace("XACML request:\n" + writer.toString());
            }
            catch (WritingException e) {
                // ignore
            }
        }

        // send request to PDP
        ResponseCtx response = pdp.evaluate(request);

        // dump XACML response
        if (log.isTraceEnabled()) {
            StringWriter writer = new StringWriter();
            try {
                response.marshal(writer);
                log.trace("XACML response:\n" + writer.toString());
            }
            catch (WritingException e) {
                // ignore
            }
        }

        // extract decision from the response
        ResponseType responseType = response.getResponse();
        ResultType result = responseType.getResults().get(0);
        DecisionType decision = result.getDecision();

        return decision.value().equals("Permit");
    }

    public void redeployPolicy(Policy policy) {
        if (isDeployed(policy.getXacmlPolicy())) {
            policyRepository.undeploy(policy.getXacmlPolicy().getId());
        }

        policyRepository.deploy(policy.getXacmlPolicy());
    }

    private Policy createPolicy(Subject subject) {
        return PolicyFactory.createPolicy(subject);
    }

    public Policy getPolicy(Subject subject) {
        List<Evaluatable> evaluatables = policyRepository.getDeployment();
        for (Evaluatable evaluatable : evaluatables) {
            try {
                SubjectsType subjectsType = evaluatable.getTarget().getSubjects();
                List<SubjectType> subjectType = subjectsType.getSubjects();
                String uuid = subjectType.get(0).getSubjectMatches().get(0).getAttributeValue().getContent().get(0)
                        .toString();
                if (uuid.equals(subject.getId())) {
                    return new Policy((PolicyType) evaluatable);
                }
            }
            catch (Exception e) {
                continue;
            }
        }

        // create new policy if it doesn't exist yet
        return PolicyFactory.createPolicy(subject);
    }

    public Policy getPolicy(String ruleId) {
        String[] arr = ruleId.split("\\.");
        String policyId = arr[0];
        PolicyType xacmlPolicy = (PolicyType) prp.getEvaluatable(new EvaluatableIDImpl(policyId));
        return new Policy(xacmlPolicy);
    }

    public Rule getRule(String ruleId) throws Exception {
        String[] arr = ruleId.split("\\.");
        String policyId = arr[0];
        PolicyType xacmlPolicy = (PolicyType) prp.getEvaluatable(new EvaluatableIDImpl(policyId));

        for (RuleType xacmlRule : xacmlPolicy.getUnorderedRules()) {
            if (xacmlRule.getRuleId().equals(ruleId)) {
                return new Rule(xacmlPolicy.getTarget(), xacmlRule);
            }
        }
        return null;
    }

    public List<Rule> getRules(Subject subject) throws Exception {
        Policy policy = getPolicy(subject);
        return policy.getRules();
    }

    private boolean isDeployed(Evaluatable evaluatable) {
        try {
            prp.getEvaluatable(evaluatable.getId());
        }
        catch (PolicyRepositoryException e) {
            return false;
        }

        return true;
    }
}

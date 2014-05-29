package org.herasaf.xacml.core.combiningAlgorithm.rule.impl;

import org.herasaf.xacml.core.combiningAlgorithm.rule.RuleUnorderedCombiningAlgorithm;
import org.herasaf.xacml.core.context.EvaluationContext;
import org.herasaf.xacml.core.context.XACMLDefaultStatusCode;
import org.herasaf.xacml.core.context.impl.DecisionType;
import org.herasaf.xacml.core.context.impl.RequestType;
import org.herasaf.xacml.core.policy.impl.ResourceType;
import org.herasaf.xacml.core.policy.impl.RuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleHierarchicalAlgorithm extends RuleUnorderedCombiningAlgorithm {

    public static final String ID = "urn:contrail:rule-combining-algorithm:hierarchical";

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(RuleHierarchicalAlgorithm.class);


    /**
     * {@inheritDoc}
     */
    @Override
    public String getCombiningAlgorithmId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    public DecisionType evaluateRuleList(final RequestType request, final List<RuleType> rules,
                                         final EvaluationContext evaluationContext) {

        if (rules == null) {
            // It is an illegal state if the list containing the rules is
            // null.
            logger.error("the rules list was null. This is an illegal state.");
            evaluationContext.updateStatusCode(XACMLDefaultStatusCode.SYNTAX_ERROR);
            return DecisionType.INDETERMINATE;
        }

        Map<String, RuleType> rulesMap = new HashMap<String, RuleType>();
        for (RuleType rule : rules) {
            for (ResourceType resource : rule.getTarget().getResources().getResources()) {
                String path = resource.getResourceMatches().get(0).getAttributeValue().getContent().get(0).toString();
                path = trimPath(path);
                rulesMap.put(path, rule);
            }
        }

        String requestPath = request.getResources().get(0).getAttributes().get(0).getAttributeValues().get(0)
                .getContent().get(0).toString();
        requestPath = trimPath(requestPath);

        while (!requestPath.equals("")) {
            if (rulesMap.containsKey(requestPath)) {
                RuleType rule = rulesMap.get(requestPath);
                DecisionType decision = evaluateSingleRule(request, rule, evaluationContext);
                if (!decision.equals(DecisionType.NOT_APPLICABLE)) {
                    return decision;
                }
            }

            // move one level up
            int lastSlashI = requestPath.lastIndexOf('/', requestPath.length() - 2);
            if (lastSlashI == -1) {
                return DecisionType.NOT_APPLICABLE;
            }
            else {
                requestPath = requestPath.substring(0, lastSlashI);
            }
        }

        return DecisionType.NOT_APPLICABLE;
    }

    private DecisionType evaluateSingleRule(RequestType request, RuleType rule, EvaluationContext evaluationContext) {
        evaluationContext.resetStatus();

        if (logger.isDebugEnabled()) {
            MDC.put(MDC_RULE_ID, rule.getRuleId());
            logger.debug("Starting evaluation of: {}", rule.getRuleId());
        }

        DecisionType decision = this.evaluateRule(request, rule, evaluationContext);
        if (logger.isDebugEnabled()) {
            MDC.put(MDC_RULE_ID, rule.getRuleId());
            logger.debug("Evaluation of {} was: {}", rule.getRuleId(), decision.toString());
            MDC.remove(MDC_RULE_ID);
        }

        return decision;
    }

    private String trimPath(String path) {
        path = path.trim();
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        else {
            return path;
        }
    }
}

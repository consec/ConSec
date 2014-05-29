package org.consec.authz.herasaf.pdp;

import org.apache.log4j.Logger;
import org.herasaf.xacml.core.WritingException;
import org.herasaf.xacml.core.dataTypeAttribute.impl.DateTimeDataTypeAttribute;
import org.herasaf.xacml.core.dataTypeAttribute.impl.StringDataTypeAttribute;
import org.herasaf.xacml.core.function.impl.bagFunctions.DateTimeOneAndOnlyFunction;
import org.herasaf.xacml.core.function.impl.equalityPredicates.StringEqualFunction;
import org.herasaf.xacml.core.function.impl.logicalFunctions.ANDFunction;
import org.herasaf.xacml.core.function.impl.nonNumericComparisonFunctions.DateTimeGreaterThanFunction;
import org.herasaf.xacml.core.function.impl.nonNumericComparisonFunctions.DateTimeLessThanFunction;
import org.herasaf.xacml.core.function.impl.specialMatchFunctions.DescendantOrSelfMatchFunction;
import org.herasaf.xacml.core.policy.PolicyMarshaller;
import org.herasaf.xacml.core.policy.impl.*;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class Policy {
    private static Logger log = Logger.getLogger(Policy.class);
    private static final String DATE_PATTERN_XACML = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static ObjectFactory objectFactory;
    private PolicyType xacmlPolicy;

    static {
        objectFactory = new ObjectFactory();
    }

    public Policy(PolicyType xacmlPolicy) {
        this.xacmlPolicy = xacmlPolicy;
    }

    public String serialize() throws WritingException {
        StringWriter writer = new StringWriter();
        PolicyMarshaller.marshal(xacmlPolicy, writer);
        return writer.toString();
    }

    public PolicyType getXacmlPolicy() {
        return xacmlPolicy;
    }

    public String addRule(Rule rule) {
        String ruleId = xacmlPolicy.getPolicyId() + "." + UUID.randomUUID().toString();
        rule.setRuleId(ruleId);
        RuleType xacmlRule = createRule(rule);
        xacmlPolicy.getAdditionalInformation().add(xacmlRule);
        return rule.getRuleId();
    }

    public void updateRule(String ruleId, Rule newRuleData) throws Exception {
        RuleType oldRule = null;
        for (RuleType rule : xacmlPolicy.getUnorderedRules()) {
            if (rule.getRuleId().equals(ruleId)) {
                oldRule = rule;
                break;
            }
        }
        if (oldRule == null) {
            throw new Exception(String.format("Update failed: rule with ID '%s' doesn't exist.", ruleId));
        }
        RuleType newRule = createRule(newRuleData);
        newRule.setRuleId(ruleId);

        xacmlPolicy.getAdditionalInformation().remove(oldRule);
        xacmlPolicy.getAdditionalInformation().add(newRule);
    }

    public void removeRule(String ruleId) throws Exception {
        RuleType ruleToDelete = null;
        for (RuleType rule : xacmlPolicy.getUnorderedRules()) {
            if (rule.getRuleId().equals(ruleId)) {
                ruleToDelete = rule;
                break;
            }
        }
        if (ruleToDelete == null) {
            throw new Exception(String.format("Failed to delete: rule with ID '%s' doesn't exist.", ruleId));
        }
        else {
            xacmlPolicy.getAdditionalInformation().remove(ruleToDelete);
        }
    }

    public Rule getRule(String ruleId) throws Exception {
        SubjectType policySubject = xacmlPolicy.getTarget().getSubjects().getSubjects().get(0);
        for (RuleType rule : xacmlPolicy.getUnorderedRules()) {
            if (rule.getRuleId().equals(ruleId)) {
                return convertRule(policySubject, rule);
            }
        }

        return null;
    }

    public List<Rule> getRules() {
        SubjectType policySubject = xacmlPolicy.getTarget().getSubjects().getSubjects().get(0);
        List<Rule> rules = new ArrayList<Rule>();
        for (RuleType rule : xacmlPolicy.getUnorderedRules()) {
            rules.add(convertRule(policySubject, rule));
        }

        return rules;
    }

    private Rule convertRule(SubjectType policySubject, RuleType xacmlRule) {
        Rule rule = new Rule();
        rule.setRuleId(xacmlRule.getRuleId());
        rule.setDescription(xacmlRule.getDescription());

        String attrId = policySubject.getSubjectMatches().get(0).getAttributeDesignator().getAttributeId();
        Subject subject;
        if (attrId.equals(Consts.SUBJECT_USER_ID_ATTR)) {
            String id = policySubject.getSubjectMatches().get(0).getAttributeValue().getContent().get(0).toString();
            subject = new Subject(Subject.Type.USER, id);
        }
        else if (attrId.equals(Consts.SUBJECT_GROUP_ID_ATTR)) {
            String id = policySubject.getSubjectMatches().get(0).getAttributeValue().getContent().get(0).toString();
            subject = new Subject(Subject.Type.GROUP, id);
        }
        else if (attrId.equals(Consts.SUBJECT_ROLE_ID_ATTR)) {
            String id = policySubject.getSubjectMatches().get(0).getAttributeValue().getContent().get(0).toString();
            subject = new Subject(Subject.Type.ROLE, id);
        }
        else {
            throw new RuntimeException("Invalid attribute id: " + attrId);
        }
        rule.setSubject(subject);

        for (ResourceType resource : xacmlRule.getTarget().getResources().getResources()) {
            String resourceURI = (String) resource.getResourceMatches().get(0).getAttributeValue().getContent().get(0);
            rule.getResourceURIs().add(resourceURI);
        }

        for (ActionType action : xacmlRule.getTarget().getActions().getActions()) {
            String value = (String) action.getActionMatches().get(0).getAttributeValue().getContent().get(0);
            rule.getActions().add(value);
        }

        // TODO: startTime and endTime
        // rule.getCondition().getExpression().getValue()
        return rule;
    }

    private RuleType createRule(Rule rule) {
        RuleType xacmlRule = new RuleType();
        xacmlRule.setRuleId(rule.getRuleId());
        xacmlRule.setEffect(EffectType.PERMIT);
        xacmlRule.setDescription(rule.getDescription());

        // set rule target
        TargetType target = new TargetType();
        xacmlRule.setTarget(target);

        // target resource
        ResourcesType resources = new ResourcesType();
        target.setResources(resources);

        for (String resourceURI : rule.getResourceURIs()) {
            ResourceType resource = new ResourceType();
            resources.getResources().add(resource);
            ResourceMatchType resourceMatch = new ResourceMatchType();
            resource.getResourceMatches().add(resourceMatch);
            resourceMatch.setMatchFunction(new DescendantOrSelfMatchFunction());

            AttributeValueType rValue = new AttributeValueType();
            resourceMatch.setAttributeValue(rValue);
            rValue.setDataType(new StringDataTypeAttribute());
            resourceMatch.setAttributeValue(rValue);
            rValue.getContent().add(resourceURI);

            ResourceAttributeDesignatorType rDesignator = new ResourceAttributeDesignatorType();
            resourceMatch.setResourceAttributeDesignator(rDesignator);
            rDesignator.setAttributeId(Consts.RESOURCE_ID_ATTR);
            rDesignator.setDataType(new StringDataTypeAttribute());
        }

        // target action
        ActionsType actions = new ActionsType();
        target.setActions(actions);

        for (String action : rule.getActions()) {
            ActionType actionT = new ActionType();
            actions.getActions().add(actionT);
            ActionMatchType actionMatch = new ActionMatchType();
            actionT.getActionMatches().add(actionMatch);
            actionMatch.setMatchFunction(new StringEqualFunction());

            AttributeValueType aValue = new AttributeValueType();
            actionMatch.setAttributeValue(aValue);
            aValue.setDataType(new StringDataTypeAttribute());
            actionMatch.setAttributeValue(aValue);
            aValue.getContent().add(action);

            ActionAttributeDesignatorType aDesignator = new ActionAttributeDesignatorType();
            actionMatch.setActionAttributeDesignator(aDesignator);
            aDesignator.setAttributeId(Consts.ACTION_NAME_ATTR);
            aDesignator.setDataType(new StringDataTypeAttribute());
        }

        // rule condition
        if (rule.getStartTime() != null || rule.getEndTime() != null) {
            ConditionType condition = new ConditionType();
            xacmlRule.setCondition(condition);
            ApplyType andFunction = new ApplyType();
            andFunction.setFunction(new ANDFunction());
            condition.setExpression(objectFactory.createApply(andFunction));

            if (rule.getStartTime() != null) {
                ApplyType function = createFunctionDateGreaterThan(rule.getStartTime());
                andFunction.getExpressions().add(objectFactory.createApply(function));
            }
            if (rule.getEndTime() != null) {
                ApplyType function = createFunctionDateLessThan(rule.getEndTime());
                andFunction.getExpressions().add(objectFactory.createApply(function));
            }
        }

        return xacmlRule;
    }

    private ApplyType createFunctionDateGreaterThan(Date startTime) {
        // dateTime-greater-than function
        ApplyType greaterThanFunction = new ApplyType();
        greaterThanFunction.setFunction(new DateTimeGreaterThanFunction());

        // argument 1: function dateTime-one-and-only
        ApplyType apply2 = new ApplyType();
        apply2.setFunction(new DateTimeOneAndOnlyFunction());
        EnvironmentAttributeDesignatorType designator = new EnvironmentAttributeDesignatorType();
        designator.setDataType(new DateTimeDataTypeAttribute());
        designator.setAttributeId(Consts.ENVIRONMENT_CURRENT_TIME_ATTR);
        apply2.getExpressions().add(objectFactory.createEnvironmentAttributeDesignator(designator));
        greaterThanFunction.getExpressions().add(objectFactory.createApply(apply2));

        // argument 2: AttributeValue
        AttributeValueType attrValue = new AttributeValueType();
        attrValue.setDataType(new DateTimeDataTypeAttribute());

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN_XACML);
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(timeZone);

        attrValue.getContent().add(sdf.format(startTime));
        greaterThanFunction.getExpressions().add(objectFactory.createAttributeValue(attrValue));

        return greaterThanFunction;
    }

    private ApplyType createFunctionDateLessThan(Date endTime) {
        // dateTime-less-than function
        ApplyType lessThanFunction = new ApplyType();
        lessThanFunction.setFunction(new DateTimeLessThanFunction());

        // argument 1: function dateTime-one-and-only
        ApplyType apply2 = new ApplyType();
        apply2.setFunction(new DateTimeOneAndOnlyFunction());
        EnvironmentAttributeDesignatorType designator = new EnvironmentAttributeDesignatorType();
        designator.setDataType(new DateTimeDataTypeAttribute());
        designator.setAttributeId(Consts.ENVIRONMENT_CURRENT_TIME_ATTR);
        apply2.getExpressions().add(objectFactory.createEnvironmentAttributeDesignator(designator));
        lessThanFunction.getExpressions().add(objectFactory.createApply(apply2));

        // argument 2: AttributeValue
        AttributeValueType attrValue = new AttributeValueType();
        attrValue.setDataType(new DateTimeDataTypeAttribute());

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN_XACML);
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(timeZone);
        attrValue.getContent().add(sdf.format(endTime));
        lessThanFunction.getExpressions().add(objectFactory.createAttributeValue(attrValue));

        return lessThanFunction;
    }
}

package org.consec.authz.herasaf.pdp;

import org.apache.log4j.Logger;
import org.herasaf.xacml.core.combiningAlgorithm.rule.impl.RuleHierarchicalAlgorithm;
import org.herasaf.xacml.core.dataTypeAttribute.impl.StringDataTypeAttribute;
import org.herasaf.xacml.core.function.impl.equalityPredicates.StringEqualFunction;
import org.herasaf.xacml.core.policy.impl.*;

import java.util.UUID;

public class PolicyFactory {
    private static Logger log = Logger.getLogger(PolicyFactory.class);

    public static Policy createPolicy(Subject subject) {
        PolicyType xacmlPolicy = new PolicyType();
        xacmlPolicy.setPolicyId(UUID.randomUUID().toString());
        xacmlPolicy.setCombiningAlg(new RuleHierarchicalAlgorithm());

        // set policy target (subject)
        TargetType target = new TargetType();
        xacmlPolicy.setTarget(target);

        SubjectsType subjects = new SubjectsType();
        target.setSubjects(subjects);

        SubjectType policySubject = new SubjectType();
        subjects.getSubjects().add(policySubject);
        SubjectMatchType subjectMatch = new SubjectMatchType();
        policySubject.getSubjectMatches().add(subjectMatch);
        subjectMatch.setMatchFunction(new StringEqualFunction());

        AttributeValueType sValue = new AttributeValueType();
        subjectMatch.setAttributeValue(sValue);
        sValue.setDataType(new StringDataTypeAttribute());
        subjectMatch.setAttributeValue(sValue);
        sValue.getContent().add(subject.getId());

        SubjectAttributeDesignatorType sDesignator = new SubjectAttributeDesignatorType();
        subjectMatch.setSubjectAttributeDesignator(sDesignator);
        sDesignator.setAttributeId(subject.getAttributeId());
        sDesignator.setDataType(new StringDataTypeAttribute());

        return new Policy(xacmlPolicy);
    }
}

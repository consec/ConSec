package org.consec.authz.herasaf.pdp.core;

import com.google.gson.Gson;
import org.consec.authz.herasaf.pdp.utils.JsonUtils;
import org.herasaf.xacml.core.policy.impl.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Rule {
    private String ruleId;
    private String description;
    private Subject subject;
    private List<String> resourceURIs;
    private List<String> actions;
    private Date startTime;
    private Date endTime;

    public Rule() {
        resourceURIs = new ArrayList<String>();
        actions = new ArrayList<String>();
    }

    public Rule(TargetType target, RuleType rule) throws Exception {
        ruleId = rule.getRuleId();
        description = rule.getDescription();

        SubjectType policySubject = target.getSubjects().getSubjects().get(0);
        String attrId = policySubject.getSubjectMatches().get(0).getAttributeDesignator().getAttributeId();
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
            throw new Exception("Invalid attribute id: " + attrId);
        }

        resourceURIs = new ArrayList<String>();
        for (ResourceType resource : rule.getTarget().getResources().getResources()) {
            String resourceURI = (String) resource.getResourceMatches().get(0).getAttributeValue().getContent().get(0);
            resourceURIs.add(resourceURI);
        }

        actions = new ArrayList<String>();
        for (ActionType action : rule.getTarget().getActions().getActions()) {
            String value = (String) action.getActionMatches().get(0).getAttributeValue().getContent().get(0);
            actions.add(value);
        }

        // TODO: startTime and endTime
        // rule.getCondition().getExpression().getValue()
    }

    public String getRuleId() {
        return ruleId;
    }

    protected void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public List<String> getResourceURIs() {
        return resourceURIs;
    }

    public void setResourceURIs(List<String> resourceURIs) {
        this.resourceURIs = resourceURIs;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public static Rule fromJSON(String json) {
        Gson gson = JsonUtils.getInstance().getGson();
        return gson.fromJson(json, Rule.class);
    }

    public String toJSON() {
        Gson gson = JsonUtils.getInstance().getGson();
        return gson.toJson(this);
    }
}

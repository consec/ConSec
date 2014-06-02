package org.consec.authz.xacml.common.xacmlrequest;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class XACMLRequest {
    private SubjectList subjectList;
    private String resource;
    private Action action;

    public XACMLRequest(SubjectList subjectList, String resource, Action action) {
        this.subjectList = subjectList;
        this.resource = resource;
        this.action = action;
    }

    public SubjectList getSubjectList() {
        return subjectList;
    }

    public String getResource() {
        return resource;
    }

    public Action getAction() {
        return action;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("subject", subjectList.toJson());
        o.put("resource", resource);
        o.put("action", action.name());
        return o;
    }
}

package org.consec.authz.xacml.common.xacmlrequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class SubjectList {
    List<Subject> subjectList;

    public SubjectList() {
        subjectList = new ArrayList<Subject>();
    }

    public List<Subject> getSubjectList() {
        return subjectList;
    }

    public void setSubjectList(List<Subject> subjectList) {
        this.subjectList = subjectList;
    }

    public void addSubject(Subject subject) {
        subjectList.add(subject);
    }

    public void addSubject(Subject[] subjects) {
        for (Subject subject : subjects) {
            subjectList.add(subject);
        }
    }

    public static SubjectList fromJson(JSONArray jsonArray) throws JSONException {
        SubjectList subjectList = new SubjectList();
        for (int i=0; i<jsonArray.length(); i++) {
            Subject subject = Subject.fromJson(jsonArray.getJSONObject(i));
            subjectList.addSubject(subject);
        }
        return subjectList;
    }

    public JSONArray toJson() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Subject subject : subjectList) {
            jsonArray.put(subject.toJson());
        }
        return  jsonArray;
    }
}

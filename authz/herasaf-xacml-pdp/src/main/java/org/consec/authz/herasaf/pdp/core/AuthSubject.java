package org.consec.authz.herasaf.pdp.core;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AuthSubject {
    List<Subject> subjectList;

    public AuthSubject() {
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

    public static AuthSubject fromJson(JSONArray jsonArray) throws JSONException {
        AuthSubject authSubject = new AuthSubject();
        for (int i=0; i<jsonArray.length(); i++) {
            Subject subject = Subject.fromJson(jsonArray.getJSONObject(i));
            authSubject.addSubject(subject);
        }
        return authSubject;
    }
}

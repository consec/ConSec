package org.consec.authz.herasaf.pdp;

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
}

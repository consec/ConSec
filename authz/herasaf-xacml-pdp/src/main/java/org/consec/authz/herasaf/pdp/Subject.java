package org.consec.authz.herasaf.pdp;

public class Subject {
    private Type type;
    private String id;

    public Subject(Type type, String id) {
        this.type = type;
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getAttributeId() {
        return type.getAttributeId();
    }

    public enum Type {
        USER(Consts.SUBJECT_USER_ID_ATTR),
        ROLE(Consts.SUBJECT_ROLE_ID_ATTR),
        GROUP(Consts.SUBJECT_GROUP_ID_ATTR);

        private String attributeId;

        Type(String attributeId) {
            this.attributeId = attributeId;
        }

        public String getAttributeId() {
            return attributeId;
        }
    }
}

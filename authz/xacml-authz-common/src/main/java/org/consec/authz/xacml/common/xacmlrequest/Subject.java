package org.consec.authz.xacml.common.xacmlrequest;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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

    public static Subject fromJson(JSONObject o) throws JSONException {
        Type type = Type.valueOf(o.getString("type"));
        String id = o.getString("id");
        return new Subject(type, id);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("type", type.name());
        o.put("id", id);
        return o;
    }
}

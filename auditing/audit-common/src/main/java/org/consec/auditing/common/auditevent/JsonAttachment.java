package org.consec.auditing.common.auditevent;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializable;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;

public class JsonAttachment extends Attachment implements JsonSerializable {

    public JsonAttachment() {
    }

    public JsonAttachment(String name) {
        super(name, "application/json", new JSONObject());
    }

    public void put(String key, Object value) throws JSONException {
        ((JSONObject)content).put(key, value);
    }


    public String toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("name", name);
        o.put("contentType", contentType);
        o.put("content", ((JSONObject)content).toString());
        return o.toString();
    }

    @Override
    public void serialize(JsonGenerator jsonGen, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonGen.writeStartObject();
        jsonGen.writeStringField("name", name);
        jsonGen.writeStringField("contentType", contentType);
        jsonGen.writeStringField("content", ((JSONObject)content).toString());
        jsonGen.writeEndObject();
    }
}

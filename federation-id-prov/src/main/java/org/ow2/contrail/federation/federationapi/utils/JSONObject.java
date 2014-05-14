/**
 *
 */
package org.ow2.contrail.federation.federationapi.utils;

import org.json.JSONException;

/**
 * @author ales
 */
public class JSONObject extends org.json.JSONObject {

    public JSONObject() throws JSONException {
        super();
    }

    public JSONObject(String content) throws JSONException {
        super(content);
    }

    /**
     * Get the value object associated with a key.
     *
     * @param key A key string.
     * @return The object associated with the key.
     * @throws JSONException if the key is not found.
     */
    public Object get(String key) throws JSONException {
        Object value = super.get(key);

        if (value == JSONObject.NULL) {
            return null;
        }

        return value;
    }

    /**
     * This is extension for JSONObject since it does provide a way to
     * store values as JSONObject.NULL when the value is null.
     * <p/>
     * Put a key/value pair in the JSONObject. If the value is null,
     * then the key will NOT! be removed from the JSONObject if it is present.
     *
     * @param key   A key string.
     * @param value An object which is the value. It should be of one of these
     *              types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,
     *              or the JSONObject.NULL object.
     * @return this.
     * @throws JSONException If the value is non-finite number
     *                       or if the key is null.
     */
    @Override
    public org.json.JSONObject put(String key, Object value) throws JSONException {
        if (value == null) {
            value = JSONObject.NULL;
        }
        return super.put(key, value);
    }
}

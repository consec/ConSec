package org.ow2.contrail.federation.federationapi.utils;

import com.google.gson.*;
import org.apache.log4j.Logger;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JsonUtils {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static Logger log = Logger.getLogger(JsonUtils.class);

    private static JsonUtils instance;
    private Gson gson;

    private JsonUtils() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Date.class, new DateSerializer());
        gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());

        gson = gsonBuilder.create();
        log.info("Gson created.");

    }

    public static JsonUtils getInstance() {
        if (instance == null) {
            instance = new JsonUtils();
        }
        return instance;
    }

    public Gson getGson() {
        return gson;
    }

    private static class DateSerializer implements JsonSerializer<Date> {

        public JsonElement serialize(Date date, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonPrimitive dateprim = null;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
                String dateString = sdf.format(date);
                dateprim = new JsonPrimitive(dateString);
            }
            catch (Exception e) {
                e.printStackTrace();
                log.error("DateDeserializer failed for dateString ", e);

                throw new JsonParseException(e.getMessage());
            }
            return dateprim;
        }
    }

    private static class DateDeserializer implements JsonDeserializer<Date> {
        public Date deserialize(JsonElement json, Type typeOfT,
                                JsonDeserializationContext context) throws JsonParseException {
            String dateString = json.getAsJsonPrimitive().getAsString();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
                return sdf.parse(dateString);
            }
            catch (ParseException e) {

                e.printStackTrace();
                log.error("DateDeserializer failed for dateString "
                        + dateString, e);

                throw new JsonParseException(e.getMessage());
            }
        }
    }
}

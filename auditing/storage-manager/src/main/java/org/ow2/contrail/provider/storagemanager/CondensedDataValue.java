package org.ow2.contrail.provider.storagemanager;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;

public class CondensedDataValue implements JsonSerializableWithType {
    private double min;
    private double max;
    private double avg;

    public CondensedDataValue(double min, double max, double avg) {
        this.min = min;
        this.max = max;
        this.avg = avg;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getAvg() {
        return avg;
    }

    @Override
    public void serializeWithType(JsonGenerator jsonGenerator, SerializerProvider serializerProvider,
                                  TypeSerializer typeSerializer) throws IOException, JsonProcessingException {
        jsonGenerator.writeStartArray();
        jsonGenerator.writeNumber(min);
        jsonGenerator.writeNumber(avg);
        jsonGenerator.writeNumber(max);
        jsonGenerator.writeEndArray();
    }

    @Override
    public void serialize(JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        serializeWithType(jsonGenerator, serializerProvider, null);
    }
}

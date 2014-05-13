package org.ow2.contrail.provider.storagemanager.aggregates;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MinAggregate implements AggregateFunction {
    private String metric;
    private Number minValue;
    private Date timestamp;
    private String sid;

    public MinAggregate(String metric) {
        this.metric = metric;
        minValue = null;
    }

    @Override
    public void addValue(String sid, Date timestamp, Map<String, Object> metricValues) throws Exception {
        Object value = metricValues.get(metric);
        if (!(value instanceof Number)) {
            throw new Exception(
                    String.format("Value '%s' is of invalid type for the aggregate function MinAggregate.",
                            value.toString()));
        }

        Number numValue = (Number) value;
        if (minValue == null ||
                numValue.doubleValue() < minValue.doubleValue()) {
            minValue = numValue;
            this.timestamp = timestamp;
            this.sid = sid;
        }
    }

    @Override
    public Map<String, Object> getAggregateValue() {
        Map<String, Object> result = new HashMap<String, Object>();
        if (minValue != null) {
            result.put("value", minValue);
            result.put("timestamp", timestamp);
            result.put("sid", sid);
        }
        return result;
    }
}

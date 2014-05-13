package org.ow2.contrail.provider.storagemanager.aggregates;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MaxAggregate implements AggregateFunction {
    private Number maxValue;
    private Date timestamp;
    private String sid;
    private String metric;

    public MaxAggregate(String metric) {
        this.metric = metric;
        maxValue = null;
    }

    @Override
    public void addValue(String sid, Date timestamp, Map<String, Object> metricValues) throws Exception {
        Object value = metricValues.get(metric);
        if (!(value instanceof Number)) {
            throw new Exception(
                    String.format("Value '%s' is of invalid type for the aggregate function MaxAggregate.",
                            value.toString()));
        }

        Number numValue = (Number) value;
        if (maxValue == null ||
                numValue.doubleValue() > maxValue.doubleValue()) {
            maxValue = numValue;
            this.timestamp = timestamp;
            this.sid = sid;
        }
    }

    @Override
    public Map<String, Object> getAggregateValue() {
        Map<String, Object> result = new HashMap<String, Object>();
        if (maxValue != null) {
            result.put("value", maxValue);
            result.put("timestamp", timestamp);
            result.put("sid", sid);
        }
        return result;

    }
}

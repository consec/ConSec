package org.ow2.contrail.provider.storagemanager.aggregates;

import java.util.*;

public class MeanAggregate implements AggregateFunction {
    private String metric;
    private double sum;
    private int count;
    private List<Double> values;

    public MeanAggregate(String metric) {
        this.metric = metric;
        sum = 0;
        count = 0;
        values = new ArrayList<Double>();
    }

    @Override
    public void addValue(String sid, Date timestamp, Map<String, Object> metricValues) throws Exception {
        Object value = metricValues.get(metric);
        if (!(value instanceof Number)) {
            throw new Exception(
                    String.format("Value '%s' is of invalid type for the aggregate function MeanAggregate.",
                            value.toString()));
        }

        Number numValue = (Number) value;
        sum += numValue.doubleValue();
        count++;
        values.add(numValue.doubleValue());
    }

    @Override
    public Map<String, Object> getAggregateValue() {
        Map<String, Object> result = new HashMap<String, Object>();
        if (count > 0) {
            double mean = sum / count;
            result.put("mean", mean);

            // stddev
            double deviationsSum = 0;
            for (double value : values) {
                deviationsSum += Math.pow(value - mean, 2);
            }
            double stddev = Math.sqrt(deviationsSum / count);
            result.put("stddev", stddev);
        }
        return result;
    }
}

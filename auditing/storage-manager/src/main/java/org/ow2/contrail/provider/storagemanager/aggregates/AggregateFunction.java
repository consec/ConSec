package org.ow2.contrail.provider.storagemanager.aggregates;

import java.util.Date;
import java.util.Map;

public interface AggregateFunction {

    public void addValue(String sid, Date timestamp,  Map<String, Object> metricValues) throws Exception;

    public Object getAggregateValue();
}

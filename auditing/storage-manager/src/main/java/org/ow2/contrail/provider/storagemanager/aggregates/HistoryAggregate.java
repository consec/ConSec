package org.ow2.contrail.provider.storagemanager.aggregates;

import java.util.*;

public class HistoryAggregate implements AggregateFunction {
    private ArrayList<Map<String, Object>> history;
    private List<String> metrics;

    public HistoryAggregate(List<String> metrics) {
        this.metrics = metrics;
        history = new ArrayList<Map<String, Object>>();
    }

    @Override
    public void addValue(String sid, Date timestamp, Map<String, Object> metricValues) throws Exception {
        Map<String, Object> historyItem = new HashMap<String, Object>();
        historyItem.put("time", timestamp);
        for (String metric : metrics) {
            historyItem.put(metric, metricValues.get(metric));
        }
        history.add(historyItem);
    }

    @Override
    public List<Map<String, Object>> getAggregateValue() {
        return history;
    }
}

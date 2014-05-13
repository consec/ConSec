package org.ow2.contrail.provider.storagemanager.aggregates;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class HistoryJsonAggregate implements AggregateFunction {
    private JSONArray history;
    private List<String> metrics;

    public HistoryJsonAggregate(String metric) {
        this.metrics = new ArrayList<String>();
        this.metrics.add(metric);
        history = new JSONArray();
    }

    public HistoryJsonAggregate(List<String> metrics) {
        this.metrics = metrics;
        history = new JSONArray();
    }

    @Override
    public void addValue(String sid, Date timestamp, Map<String, Object> metricValues) throws Exception {
        JSONObject historyItem = new JSONObject();
        historyItem.put("time", timestamp);
        JSONObject metricsObj = new JSONObject();
        for (String metric : metrics) {
            metricsObj.put(metric, metricValues.get(metric));
        }
        historyItem.put("metrics", metricsObj);
        history.put(historyItem);
    }

    @Override
    public JSONArray getAggregateValue() {
        return history;
    }
}

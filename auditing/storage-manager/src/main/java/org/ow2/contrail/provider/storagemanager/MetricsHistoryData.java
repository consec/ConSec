package org.ow2.contrail.provider.storagemanager;

import org.ow2.contrail.provider.storagemanager.common.Metric;

import java.util.*;

public class MetricsHistoryData {
    private List<Date> timestamps = new ArrayList<Date>();
    private Map<String, List<Object>> metricsData = new HashMap<String, List<Object>>();

    public MetricsHistoryData(List<Metric> metrics) {
        for (Metric metric : metrics) {
            metricsData.put(metric.getFullName(), new ArrayList<Object>());
        }
    }

    public void addTimeValue(Date time) {
        timestamps.add(time);
    }

    public void addMetricValue(Metric metric, Object value) {
        metricsData.get(metric.getFullName()).add(value);
    }

    public List<Date> getTimeValues() {
        return timestamps;
    }

    public List<Object> getMetricValues(String metricFN) {
        return metricsData.get(metricFN);
    }

    public List<Object> getMetricValues(Metric metric) {
        return metricsData.get(metric.getFullName());
    }

    public Map<String, List<Object>> getMetricsData() {
        return metricsData;
    }

    public int size() {
        return timestamps.size();
    }
}

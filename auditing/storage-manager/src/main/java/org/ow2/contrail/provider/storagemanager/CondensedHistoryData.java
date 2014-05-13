package org.ow2.contrail.provider.storagemanager;

import org.ow2.contrail.provider.storagemanager.common.Metric;

import java.util.*;

public class CondensedHistoryData {
    private Date[] intervals;
    private Map<String, MetricsData> values = new HashMap<String, MetricsData>();
    private Date startTime;
    private Date endTime;
    private long intervalLength;
    private int numberOfIntervals;

    public CondensedHistoryData(List<String> sids, List<Metric> metrics, Date startTime, Date endTime,
                                int maxNumberOfIntervals) {
        this.startTime = startTime;
        this.endTime = endTime;
        for (String sid : sids) {
            values.put(sid, new MetricsData());
        }

        intervalLength = (endTime.getTime() - startTime.getTime()) / maxNumberOfIntervals;
        long minIntervalLength = Conf.getInstance().getHRMinIntervalLength() * 1000;

        if (intervalLength < minIntervalLength) {
            numberOfIntervals = (int) ((endTime.getTime() - startTime.getTime()) / (double) minIntervalLength);
            intervalLength = (endTime.getTime() - startTime.getTime()) / numberOfIntervals;
        }
        else {
            numberOfIntervals = maxNumberOfIntervals;
        }

        long time = startTime.getTime();
        intervals = new Date[numberOfIntervals];
        for (int i = 0; i < numberOfIntervals; i++) {
            intervals[i] = new Date(time + intervalLength / 2);
            time += intervalLength;
        }
    }

    public Date[] getIntervals() {
        return intervals;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public long getIntervalLength() {
        return intervalLength;
    }

    public int getNumberOfIntervals() {
        return numberOfIntervals;
    }

    public void storeMetricData(String sid, Metric metric, CondensedDataValue[] metricValues) {
        values.get(sid).storeMetricData(metric, metricValues);
    }

    public MetricsData getMetricsData(String sid) {
        return values.get(sid);
    }

    public Map<String, MetricsData> getValues() {
        return values;
    }

    public static class MetricsData {
        private Map<Metric,CondensedDataValue[]> values = new HashMap<Metric,CondensedDataValue[]>();

        public Map<Metric, CondensedDataValue[]> getMetricsData() {
            return values;
        }

        public CondensedDataValue[] getMetricData(Metric metric) {
            return values.get(metric);
        }

        public void storeMetricData(Metric metric, CondensedDataValue[] metricValues) {
            values.put(metric, metricValues);
        }
    }
}

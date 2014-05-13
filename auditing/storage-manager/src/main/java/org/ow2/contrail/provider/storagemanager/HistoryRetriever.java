package org.ow2.contrail.provider.storagemanager;

import com.mongodb.*;
import com.mongodb.util.JSON;
import org.apache.log4j.Logger;
import org.ow2.contrail.provider.storagemanager.common.Metric;

import java.io.IOException;
import java.util.*;

public class HistoryRetriever {
    private static Logger log = Logger.getLogger(HistoryRetriever.class);
    private DBCollection compressedCollection;
    private DBCollection rawCollection;

    public HistoryRetriever(DB db) throws IOException {
        compressedCollection = db.getCollection(Conf.COMPRESSED_COLL_NAME);
        rawCollection = db.getCollection(Conf.RAW_COLL_NAME);
    }

    public MetricsHistoryData getHistory(Metric metric, String source, String sid,
                                      Date startTime, Date endTime) throws Exception {
        List<Metric> metrics = new ArrayList<Metric>();
        metrics.add(metric);
        return getSingleGroupHistory(metrics, source, sid, startTime, endTime);
    }

    public MetricsHistoryData getHistory(List<Metric> metrics, String source,
                                      String sid, Date startTime,
                                      Date endTime) throws Exception {
        if (metrics.size() == 0) {
            throw new Exception("Metrics list is empty.");
        }
        Map<String, List<Metric>> metricsMap = new HashMap<String, List<Metric>>();
        for (Metric metric : metrics) {
            String group = metric.getGroup();
            List<Metric> groupMetrics = metricsMap.get(group);
            if (groupMetrics == null) {
                groupMetrics = new ArrayList<Metric>();
                metricsMap.put(group, groupMetrics);
            }

            groupMetrics.add(metric);
        }

        if (metricsMap.keySet().size() > 1) {
            throw new Exception("All metrics should be in the same group.");
        }

        List<Metric> groupMetrics = metricsMap.get(metricsMap.keySet().iterator().next());
        return getSingleGroupHistory(groupMetrics, source, sid, startTime, endTime);
    }

    private MetricsHistoryData getSingleGroupHistory(List<Metric> metrics, String source,
                                                  String sid, Date startTime,
                                                  Date endTime) throws Exception {

        String group = metrics.get(0).getGroup();
        for (int i = 1; i < metrics.size(); i++) {
            if (!metrics.get(i).getGroup().equals(group)) {
                throw new Exception("All metrics should be from the same group.");
            }
        }

        MetricsHistoryData history = new MetricsHistoryData(metrics);

        // first retrieve metrics history from compressed collection
        Date lastTime = null;
        DBCursor cursor = null;
        try {
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("group", group);
            searchQuery.put("source", source.toLowerCase());
            searchQuery.put("sid", sid);
            searchQuery.put("startTime", BasicDBObjectBuilder.start("$lt", endTime).get());
            if (startTime != null) {
                searchQuery.put("endTime", BasicDBObjectBuilder.start("$gte", startTime).get());
            }
            BasicDBObject sortBy = new BasicDBObject("time", 1);

            cursor = compressedCollection.find(searchQuery).sort(sortBy);

            while (cursor.hasNext()) {
                DBObject compressedRecord = cursor.next();
                byte[] data = (byte[]) compressedRecord.get("data");
                String jsonString = Archiver.decompressGzip(data);

                BasicDBObject metricsData = (BasicDBObject) JSON.parse(jsonString);

                ArrayList timeList = (BasicDBList) metricsData.get("time");

                for (int i = 0; i < timeList.size(); i++) {
                    Date timestamp = new Date((Long) timeList.get(i));
                    if (startTime != null && timestamp.getTime() < startTime.getTime()) {
                        continue;
                    }
                    else if (timestamp.getTime() > endTime.getTime()) {
                        break;
                    }
                    else {
                        history.addTimeValue(timestamp);
                        for (Metric metric : metrics) {
                            Object value;
                            BasicDBList valuesList = (BasicDBList) metricsData.get(metric.getName());
                            if (valuesList != null) {
                                value = valuesList.get(i);
                            }
                            else {
                                value = null;
                            }
                            history.addMetricValue(metric, value);
                        }

                        lastTime = timestamp;
                    }
                }
            }
        }
        catch (Exception e) {
            throw new Exception("Failed to retrieve metrics history: " + e.getMessage(), e);
        }
        finally {
            if (cursor != null)
                cursor.close();
        }

        // retrieve latest metrics data from raw (uncompressed) collection if needed
        if (lastTime == null ||
                (lastTime.getTime() < endTime.getTime())) {
            try {
                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put("group", group);
                searchQuery.put("source", source.toLowerCase());
                searchQuery.put("sid", sid);
                searchQuery.put("archived", new BasicDBObject("$exists", false));
                BasicDBObject sortBy = new BasicDBObject("time", 1);

                Date startTime2 = (lastTime != null) ? lastTime : startTime;
                BasicDBObjectBuilder builder = new BasicDBObjectBuilder();
                if (startTime2 != null) {
                    builder.append("$gt", startTime2);
                }
                builder.append("$lte", endTime);
                searchQuery.put("time", builder.get());

                // fields to return
                BasicDBObject fields = new BasicDBObject();
                fields.put("time", 1);
                for (Metric metric : metrics) {
                    fields.put("metrics." + metric.getName(), 1);
                }

                cursor = rawCollection.find(searchQuery, fields).sort(sortBy);
                while (cursor.hasNext()) {
                    DBObject rawRecord = cursor.next();
                    Date timestamp = (Date) rawRecord.get("time");
                    BasicDBObject metricValues = (BasicDBObject) rawRecord.get("metrics");

                    history.addTimeValue(timestamp);
                    for (Metric metric : metrics) {
                        Object value = metricValues.get(metric.getName());
                        history.addMetricValue(metric, value);
                    }
                }
            }
            finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        // check data
        int timeValuesSize = history.getTimeValues().size();
        for (Metric metric : metrics) {
            if (history.getMetricValues(metric.getFullName()).size() != timeValuesSize) {
                throw new Exception("Data integrity check failed.");
            }
        }
        return history;
    }

    public CondensedHistoryData getCondensedHistory(List<Metric> metrics, String source,
                                                    List<String> sids, Date startTime,
                                                    Date endTime, int maxNumberOfIntervals) throws Exception {
        if (metrics.size() == 0) {
            throw new Exception("Metrics list is empty.");
        }
        Map<String, List<Metric>> metricsMap = new HashMap<String, List<Metric>>();
        for (Metric metric : metrics) {
            String group = metric.getGroup();
            List<Metric> groupMetrics = metricsMap.get(group);
            if (groupMetrics == null) {
                groupMetrics = new ArrayList<Metric>();
                metricsMap.put(group, groupMetrics);
            }

            groupMetrics.add(metric);
        }

        CondensedHistoryData historyData = new CondensedHistoryData(sids, metrics, startTime,
                endTime, maxNumberOfIntervals);
        for (String group : metricsMap.keySet()) {
            for (String sid : sids) {
                CondensedHistoryBuffer buffer = getSingleGroupCondensedHistory(
                        metricsMap.get(group), source, sid, startTime, endTime, historyData);
                for (Metric metric : buffer.getValues().keySet()) {
                    CondensedDataValue[] metricValues = buffer.getValues().get(metric);
                    historyData.storeMetricData(sid, metric, metricValues);
                }
            }
        }

        return historyData;
    }

    private CondensedHistoryBuffer getSingleGroupCondensedHistory(List<Metric> metrics, String source,
                                                                  String sid, Date startTime,
                                                                  Date endTime, CondensedHistoryData historyData) throws Exception {

        String group = metrics.get(0).getGroup();
        for (int i = 1; i < metrics.size(); i++) {
            if (!metrics.get(i).getGroup().equals(group)) {
                throw new Exception("All metrics should be from the same group.");
            }
        }

        CondensedHistoryBuffer buffer = new CondensedHistoryBuffer(metrics, historyData);

        // first retrieve metrics history from compressed collection
        Date lastTime = null;
        DBCursor cursor = null;
        try {
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("group", group);
            searchQuery.put("source", source.toLowerCase());
            searchQuery.put("sid", sid);
            searchQuery.put("startTime", BasicDBObjectBuilder.start("$lt", endTime).get());
            if (startTime != null) {
                searchQuery.put("endTime", BasicDBObjectBuilder.start("$gte", startTime).get());
            }
            BasicDBObject sortBy = new BasicDBObject("time", 1);

            cursor = compressedCollection.find(searchQuery).sort(sortBy);

            while (cursor.hasNext()) {
                DBObject compressedRecord = cursor.next();
                byte[] data = (byte[]) compressedRecord.get("data");
                String jsonString = Archiver.decompressGzip(data);

                BasicDBObject metricsData = (BasicDBObject) JSON.parse(jsonString);

                ArrayList timeList = (BasicDBList) metricsData.get("time");

                for (int i = 0; i < timeList.size(); i++) {
                    Date timestamp = new Date((Long) timeList.get(i));
                    if (startTime != null && timestamp.getTime() < startTime.getTime()) {
                        continue;
                    }
                    else if (timestamp.getTime() >= endTime.getTime()) {
                        break;
                    }
                    else {
                        for (Metric metric : metrics) {
                            Object value;
                            BasicDBList valuesList = (BasicDBList) metricsData.get(metric.getName());
                            if (valuesList != null) {
                                value = valuesList.get(i);
                            }
                            else {
                                continue;
                            }
                            if (value instanceof Number) {
                                buffer.addMetricValue(metric, (Number) value, timestamp);
                            }
                            else {
                                throw new Exception("Non-numeric metric value: " + value);
                            }
                        }

                        lastTime = timestamp;
                    }
                }
            }
        }
        catch (Exception e) {
            throw new Exception("Failed to retrieve metrics history: " + e.getMessage(), e);
        }
        finally {
            if (cursor != null)
                cursor.close();
        }

        // retrieve latest metrics data from raw (uncompressed) collection if needed
        if (lastTime == null ||
                (lastTime.getTime() < endTime.getTime())) {
            try {
                BasicDBObject searchQuery = new BasicDBObject();
                searchQuery.put("group", group);
                searchQuery.put("source", source.toLowerCase());
                searchQuery.put("sid", sid);
                searchQuery.put("archived", new BasicDBObject("$exists", false));
                BasicDBObject sortBy = new BasicDBObject("time", 1);

                Date startTime2 = (lastTime != null) ? lastTime : startTime;
                BasicDBObjectBuilder builder = new BasicDBObjectBuilder();
                if (startTime2 != null) {
                    builder.append("$gt", startTime2);
                }
                builder.append("$lt", endTime);
                searchQuery.put("time", builder.get());

                // fields to return
                BasicDBObject fields = new BasicDBObject();
                fields.put("time", 1);
                for (Metric metric : metrics) {
                    fields.put("metrics." + metric.getName(), 1);
                }

                cursor = rawCollection.find(searchQuery, fields).sort(sortBy);
                while (cursor.hasNext()) {
                    DBObject rawRecord = cursor.next();
                    Date timestamp = (Date) rawRecord.get("time");
                    BasicDBObject metricValues = (BasicDBObject) rawRecord.get("metrics");

                    for (Metric metric : metrics) {
                        Object value = metricValues.get(metric.getName());
                        if (value instanceof Number) {
                            buffer.addMetricValue(metric, (Number) value, timestamp);
                        }
                        else {
                            throw new Exception("Non-numeric metric value: " + value);
                        }
                    }
                }
            }
            finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        buffer.finish();
        return buffer;
    }

    public static class CondensedHistoryBuffer {
        private CondensedHistoryData historyData;
        private Map<Metric, Buffer> buffers = new HashMap<Metric, Buffer>();
        private Map<Metric, CondensedDataValue[]> values = new HashMap<Metric, CondensedDataValue[]>();

        public CondensedHistoryBuffer(List<Metric> metrics, CondensedHistoryData historyData) {
            this.historyData = historyData;
            for (Metric metric : metrics) {
                buffers.put(metric, new Buffer());
                values.put(metric, new CondensedDataValue[historyData.getNumberOfIntervals()]);
            }
        }

        public void addMetricValue(Metric metric, Number value, Date timestamp) {
            int intervalIndex = (int) ((timestamp.getTime() - historyData.getStartTime().getTime()) /
                    historyData.getIntervalLength());

            Buffer buffer = buffers.get(metric);
            if (buffer.isEmpty()) {
                buffer.setInterval(intervalIndex);
                buffer.addValue(value);
            }
            else {
                if (buffer.getIntervalIndex() == intervalIndex) {
                    buffer.addValue(value);
                }
                else {
                    CondensedDataValue dataValue = calculateCondensedValue(buffer);
                    storeDataValue(metric, buffer.getIntervalIndex(), dataValue);

                    buffer.clear();
                    buffer.setInterval(intervalIndex);
                    buffer.addValue(value);
                }
            }
        }

        private CondensedDataValue calculateCondensedValue(Buffer buffer) {
            if (buffer.isEmpty()) {
                return null;
            }
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            double sum = 0;
            for (Number num : buffer.getValues()) {
                double value = num.doubleValue();
                if (value < min) {
                    min = value;
                }
                if (value > max) {
                    max = value;
                }
                sum += value;
            }
            double avg = sum / buffer.getValues().size();
            return new CondensedDataValue(min, max, avg);
        }

        public void finish() {
            for (Metric metric : buffers.keySet()) {
                Buffer buffer = buffers.get(metric);
                CondensedDataValue dataValue = calculateCondensedValue(buffer);
                storeDataValue(metric, buffer.getIntervalIndex(), dataValue);
                buffer.clear();
            }
        }

        public void storeDataValue(Metric metric, int intervalIndex, CondensedDataValue dataValue) {
            values.get(metric)[intervalIndex] = dataValue;
        }

        public Map<Metric, CondensedDataValue[]> getValues() {
            return values;
        }

        public CondensedDataValue[] getMetricValues(Metric metric) {
            return values.get(metric);
        }

        private static class Buffer {
            private List<Number> values = new ArrayList<Number>();
            private int intervalIndex;

            public Buffer() {
            }

            public void addValue(Number value) {
                values.add(value);
            }

            public void clear() {
                values.clear();
            }

            public int getIntervalIndex() {
                return intervalIndex;
            }

            public void setInterval(int intervalIndex) {
                this.intervalIndex = intervalIndex;
            }

            public boolean isEmpty() {
                return values.isEmpty();
            }

            public List<Number> getValues() {
                return values;
            }
        }
    }
}

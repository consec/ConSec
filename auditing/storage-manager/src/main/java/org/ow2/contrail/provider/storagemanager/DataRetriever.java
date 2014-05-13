package org.ow2.contrail.provider.storagemanager;

import com.mongodb.*;
import com.mongodb.util.JSON;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ow2.contrail.provider.storagemanager.aggregates.AggregateFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DataRetriever {
    private static Logger log = Logger.getLogger(DataRetriever.class);
    private DBCollection compressedCollection;
    private DBCollection rawCollection;
    private DBCollection eventsCollection;

    public DataRetriever(DB db) throws IOException {
        this.compressedCollection = db.getCollection(Conf.COMPRESSED_COLL_NAME);
        this.rawCollection = db.getCollection(Conf.RAW_COLL_NAME);
        this.eventsCollection = db.getCollection(Conf.EVENTS_COLL_NAME);
    }

    public void calculateAggregateValue(String group, String metricFN, String source, String sid,
                                        Date startTime, Date endTime,
                                        AggregateFunction aggregate) throws Exception {
        List<String> metricFNs = new ArrayList<String>();
        metricFNs.add(metricFN);
        List<String> sids = new ArrayList<String>();
        sids.add(sid);
        calculateAggregateValue(group, metricFNs, source, sids, startTime, endTime, aggregate);
    }

    public void calculateAggregateValue(String group, List<String> metrics, String source, String sid,
                                        Date startTime, Date endTime,
                                        AggregateFunction aggregate) throws Exception {
        List<String> sids = new ArrayList<String>();
        sids.add(sid);
        calculateAggregateValue(group, metrics, source, sids, startTime, endTime, aggregate);
    }

    public void calculateAggregateValue(String group, String metric, String source, List<String> sids,
                                        Date startTime, Date endTime,
                                        AggregateFunction aggregate) throws Exception {
        List<String> metrics = new ArrayList<String>();
        metrics.add(metric);
        calculateAggregateValue(group, metrics, source, sids, startTime, endTime, aggregate);
    }

    public void calculateAggregateValue(String group, List<String> metrics, String source,
                                        List<String> sids, Date startTime, Date endTime,
                                        AggregateFunction aggregate) throws Exception {
        // first retrieve metrics history from compressed collection
        Date lastTime = null;
        DBCursor cursor = null;
        try {
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("group", group);
            searchQuery.put("source", source.toLowerCase());
            if (sids.size() == 1) {
                searchQuery.put("sid", sids.get(0));
            }
            else {
                JSONArray sidsArr = new JSONArray();
                sidsArr.put(sids);
                searchQuery.put("sid", new JSONObject().put("$in", sidsArr));
            }
            searchQuery.put("startTime", BasicDBObjectBuilder.start("$lt", endTime).get());
            if (startTime != null) {
                searchQuery.put("endTime", BasicDBObjectBuilder.start("$gte", startTime).get());
            }
            BasicDBObject sortBy = new BasicDBObject("time", 1);

            cursor = compressedCollection.find(searchQuery).sort(sortBy);

            while (cursor.hasNext()) {
                DBObject compressedRecord = cursor.next();
                String sid = (String) compressedRecord.get("sid");
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
                        if (metrics.size() == 1) {
                            String metric = metrics.get(0);
                            Object value = ((BasicDBList) metricsData.get(metric)).get(i);
                            HashMap<String, Object> metricValues = new HashMap<String, Object>();
                            metricValues.put(metric, value);
                            aggregate.addValue(sid, timestamp, metricValues);
                        }
                        else {
                            HashMap<String, Object> metricValues = new HashMap<String, Object>();
                            for (String metric : metrics) {
                                Object value = ((BasicDBList) metricsData.get(metric)).get(i);
                                metricValues.put(metric, value);
                            }
                            aggregate.addValue(sid, timestamp, metricValues);
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
                if (sids.size() == 1) {
                    searchQuery.put("sid", sids.get(0));
                }
                else {
                    JSONArray sidsArr = new JSONArray();
                    sidsArr.put(sids);
                    searchQuery.put("sid", new JSONObject().put("$in", sidsArr));
                }
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
                for (String metric : metrics) {
                    fields.put("metrics." + metric, 1);
                }

                cursor = rawCollection.find(searchQuery, fields).sort(sortBy);
                while (cursor.hasNext()) {
                    DBObject rawRecord = cursor.next();
                    String sid = (String) rawRecord.get("sid");
                    Date timestamp = (Date) rawRecord.get("time");
                    BasicDBObject metricValues = (BasicDBObject) rawRecord.get("metrics");

                    aggregate.addValue(sid, timestamp, metricValues);
                }
            }
            finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }

    public JSONArray getEvents(String appUuid, String userUuid) throws JSONException {
        DBCursor cursor = null;
        try {
            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("appUuid", appUuid);
            searchQuery.put("userUuid", userUuid);

            BasicDBObject sortBy = new BasicDBObject("timestamp", 1);

            cursor = eventsCollection.find(searchQuery).sort(sortBy);

            JSONArray eventsList = new JSONArray();
            while (cursor.hasNext()) {
                DBObject record = cursor.next();
                JSONObject event = new JSONObject();
                event.put("host", record.get("host"));
                event.put("oneId", record.get("oneId"));
                event.put("vepId", record.get("vepId"));
                event.put("slaId", record.get("slaId"));
                event.put("userUuid", record.get("userUuid"));
                event.put("appUuid", record.get("appUuid"));
                event.put("action", record.get("action"));
                event.put("timestamp", record.get("timestamp"));
                eventsList.put(event);
            }

            return eventsList;
        }
        finally {
            if (cursor != null)
                cursor.close();
        }

    }
}

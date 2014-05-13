package org.ow2.contrail.provider.storagemanager;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.Date;
import java.util.List;

public class Summarizer {

    public static DBObject createSummary(String metric, List<DBObject> history) {
        if (metric.equals("availability")) {
            return createEnumSummary(metric, history);
        }
        else if (metric.equals("hostname")) {
            return createEnumSummary(metric, history);
        }
        else {
            return createNumericSummary(metric, history);
        }
    }

    public static DBObject createEnumSummary(String metric, List<DBObject> history) {
        BasicDBList summary = new BasicDBList();

        Object prevValue = getValue(history.get(0), metric);
        Date prevTime = getTime(history.get(0));
        Object value = null;
        Date time = null;
        Date startTime = prevTime;
        for (int i = 1; i < history.size(); i++) {
            value = getValue(history.get(i), metric);
            time = getTime(history.get(i));

            if (!value.equals(prevValue)) {
                Date endTime = new Date((prevTime.getTime() + time.getTime()) / 2);
                DBObject o = new BasicDBObject();
                o.put("startTime", startTime);
                o.put("endTime", endTime);
                o.put("value", prevValue);
                summary.add(o);

                startTime = endTime;
            }
            prevValue = value;
            prevTime = time;
        }

        // store last record
        DBObject o = new BasicDBObject();
        o.put("startTime", startTime);
        o.put("endTime", time);
        o.put("value", value);
        summary.add(o);

        return summary;
    }

    private static DBObject createNumericSummary(String metric, List<DBObject> history) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        int count = 0;
        for (int i = 0; i < history.size(); i++) {
            Object value = getValue(history.get(i), metric);
            if (value instanceof Number) {
                double valueNum = ((Number) value).doubleValue();
                if (valueNum < min) {
                    min = valueNum;
                }
                if (valueNum > max) {
                    max = valueNum;
                }
                sum += valueNum;
                count++;
            }
        }
        DBObject summary = new BasicDBObject();
        summary.put("min", min);
        summary.put("max", max);
        summary.put("average", sum / count);

        return summary;
    }

    private static Object getValue(DBObject historyItem, String metric) {
        return ((DBObject) historyItem.get("metrics")).get(metric);
    }

    private static Date getTime(DBObject historyItem) {
        return (Date) historyItem.get("time");
    }

}

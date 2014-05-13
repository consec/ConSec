package org.ow2.contrail.provider.storagemanager;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;

public class Utils {
    private static Logger log = Logger.getLogger(ArchiverTest.class);

    public static void prepareTestData(DBCollection sourceCollection, Calendar startTime, Calendar stopTime,
                                   int interval) {
        log.trace("Generating test data in the collection " + sourceCollection.getFullName());
        String sid = "host001.test.com";
        String[] indexes = {"group", "sid", "time"};
        for (String index : indexes) {
            sourceCollection.createIndex(new BasicDBObject(index, 1));
        }

        Calendar calendar = (Calendar) startTime.clone();
        long periodLength = stopTime.getTimeInMillis() - startTime.getTimeInMillis();
        while (calendar.before(stopTime)) {
            float ratio = (float) (calendar.getTimeInMillis() - startTime.getTimeInMillis()) / periodLength;
            DBObject metrics;

            // common metrics
            DBObject commonData = new BasicDBObject();
            commonData.put("time", calendar.getTime());
            commonData.put("group", "common");
            commonData.put("source", "host");
            commonData.put("sid", sid);
            metrics = new BasicDBObject();
            metrics.put("hostname", sid.substring(0, sid.indexOf(".")));
            if (Math.random() < 0.9) {
                metrics.put("availability", 1);
            }
            else {
                metrics.put("availability", 0);
            }
            commonData.put("metrics", metrics);
            sourceCollection.insert(commonData);

            // cpu metrics
            DBObject cpuData = new BasicDBObject();
            cpuData.put("time", calendar.getTime());
            cpuData.put("group", "cpu");
            cpuData.put("source", "host");
            cpuData.put("sid", sid);
            metrics = new BasicDBObject();
            if (ratio < 0.4) {
                metrics.put("cores", 8);
            }
            else {
                metrics.put("cores", 4);
            }
            metrics.put("speed", getRndNumDouble(3300, 3));
            metrics.put("user", getRndNumDouble(100, 2));
            metrics.put("system", getRndNumDouble(100, 2));
            metrics.put("idle", getRndNumDouble(100, 2));
            metrics.put("load_one", getRndNumDouble(3, 2));
            metrics.put("load_five", getRndNumDouble(3, 2));
            cpuData.put("metrics", metrics);
            sourceCollection.insert(cpuData);

            // memory metrics
            DBObject memoryData = new BasicDBObject();
            memoryData.put("time", calendar.getTime());
            memoryData.put("group", "memory");
            memoryData.put("source", "host");
            memoryData.put("sid", sid);
            metrics = new BasicDBObject();
            metrics.put("total", getRndNumInt(4000));
            metrics.put("free", getRndNumInt(4000));
            metrics.put("used", getRndNumInt(4000));
            memoryData.put("metrics", metrics);
            sourceCollection.insert(memoryData);

            // disk metrics
            DBObject diskData = new BasicDBObject();
            diskData.put("time", calendar.getTime());
            diskData.put("group", "disk");
            diskData.put("source", "host");
            diskData.put("sid", sid);
            metrics = new BasicDBObject();
            metrics.put("available", getRndNumDouble(1000, 1));
            metrics.put("used", getRndNumDouble(1000, 1));
            diskData.put("metrics", metrics);
            sourceCollection.insert(diskData);

            // network metrics
            DBObject networkData = new BasicDBObject();
            networkData.put("time", calendar.getTime());
            networkData.put("group", "network");
            networkData.put("source", "host");
            networkData.put("sid", sid);
            metrics = new BasicDBObject();
            metrics.put("rx", getRndNumLong(10000000000L));
            metrics.put("tx", getRndNumLong(10000000000L));
            networkData.put("metrics", metrics);
            sourceCollection.insert(networkData);

            calendar.add(Calendar.SECOND, interval);
        }
        log.trace("Test data generated successfully.");
    }

    private static int getRndNumInt(int max) {
        return (int) (Math.random() * max);
    }

    private static long getRndNumLong(long max) {
        return (long) (Math.random() * max);
    }

    private static double getRndNumDouble(int max, int precision) {
        double d = Math.random() * max;
        BigDecimal bd = new BigDecimal(d).setScale(precision, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }
}

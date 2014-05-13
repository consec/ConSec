package org.ow2.contrail.provider.storagemanager;

import com.mongodb.*;
import org.ow2.contrail.provider.storagemanager.utils.DateUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class TestDataGenerator {

    public static void main(String[] args) throws IOException, ParseException {
        if (args.length != 5) {
            System.out.println("Usage:\n  TestDataGenerator <host> <port> <database> <startTime> <endTime>");
            System.exit(0);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String databaseName = args[2];
        Calendar startTime = GregorianCalendar.getInstance();
        startTime.setTime(DateUtils.parseDate(args[3]));
        Calendar endTime = GregorianCalendar.getInstance();
        endTime.setTime(DateUtils.parseDate(args[4]));

        Conf.getInstance().load("src/test/resources/storage-manager.cfg");
        MongoClient mongoClient = new MongoClient(host, port);

        try {
            new TestDataGenerator().prepareTestData(mongoClient, databaseName, startTime, endTime, 30);

            Archiver archiver = new Archiver(mongoClient, databaseName);
            archiver.compressNewData();
        }
        finally {
            mongoClient.close();
        }
    }

    private void prepareTestData(MongoClient mongoClient, String databaseName,
                                 Calendar startTime, Calendar stopTime, int interval) throws IOException {

        DB db = mongoClient.getDB(databaseName);
        DBCollection sourceCollection = db.getCollection(Conf.RAW_COLL_NAME);
        DBCollection targetCollection = db.getCollection(Conf.COMPRESSED_COLL_NAME);
        sourceCollection.drop();
        targetCollection.drop();

        System.out.println(String.format(
                "Generating test metrics data in the database %s on the host %s...", databaseName,
                mongoClient.getAddress().toString()));

        String[] sids = {"node1.xlab.si", "node2.xlab.si", "node3.xlab.si"};
        String[] indexes = {"group", "sid", "time"};
        for (String index : indexes) {
            sourceCollection.createIndex(new BasicDBObject(index, 1));
        }

        Calendar calendar = (Calendar) startTime.clone();
        long periodLength = stopTime.getTimeInMillis() - startTime.getTimeInMillis();
        while (calendar.before(stopTime)) {
            float ratio = (float) (calendar.getTimeInMillis() - startTime.getTimeInMillis()) / periodLength;
            DBObject metrics;

            double days = (double) (calendar.getTimeInMillis() - startTime.getTimeInMillis()) / 3600 / 1000 / 24;

            for (String sid : sids) {
                // common metrics
                DBObject commonData = new BasicDBObject();
                commonData.put("time", calendar.getTime());
                commonData.put("group", "common");
                commonData.put("source", "host");
                commonData.put("sid", sid);
                metrics = new BasicDBObject();
                metrics.put("hostname", sid.substring(0, sid.indexOf(".")));
                if (Math.random() < 0.9) {
                    metrics.put("availability", 0);
                }
                else {
                    metrics.put("availability", 1);
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
                metrics.put("cores", 8);

                metrics.put("speed", 3300);
                double system = getRndNumDouble(5, 2);
                metrics.put("system", system);

                if (days > 2 && days < 5 ||
                        days > 6 && days < 10 ||
                        days > 15 && days < 18 ||
                        days > 25 && days < 29 ||
                        days > 33 && days < 34 ||
                        days > 39 && days < 42) {
                    double user = 0;
                    metrics.put("user", user);
                    metrics.put("idle", 100 - system - user);
                    metrics.put("load_one", getRndNumDouble(3, 2));
                    metrics.put("load_five", getRndNumDouble(3, 2));
                }
                else {
                    double user = getRndNumDouble(100.0 - system, 2);
                    metrics.put("user", user);
                    metrics.put("idle", 100 - system - user);
                    metrics.put("load_one", getRndNumDouble(3, 2));
                    metrics.put("load_five", getRndNumDouble(3, 2));
                }

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
            }

            calendar.add(Calendar.SECOND, interval);
        }
        System.out.println("Test data generated successfully.");

        System.out.println("Archiving metrics data...");
        Archiver archiver = new Archiver(mongoClient, db.getName());
        archiver.compressNewData();
        System.out.println("Archiver finished successfully.");
    }

    private int getRndNumInt(int max) {
        return (int) (Math.random() * max);
    }

    private long getRndNumLong(long max) {
        return (long) (Math.random() * max);
    }

    private double getRndNumDouble(double max, int precision) {
        double d = Math.random() * max;
        BigDecimal bd = new BigDecimal(d).setScale(precision, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }
}

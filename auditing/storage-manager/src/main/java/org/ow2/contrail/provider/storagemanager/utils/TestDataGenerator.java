package org.ow2.contrail.provider.storagemanager.utils;

import com.mongodb.*;
import org.ow2.contrail.provider.storagemanager.Archiver;
import org.ow2.contrail.provider.storagemanager.Conf;

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
            new TestDataGenerator().prepareTestData(mongoClient, databaseName, startTime, endTime, 5);
            new TestDataGenerator().prepareTestData2(mongoClient, databaseName, startTime, endTime, 5);

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
        //sourceCollection.drop();
        //targetCollection.drop();

        System.out.println(String.format(
                "Generating test metrics data in the database %s on the host %s...", databaseName,
                mongoClient.getAddress().toString()));

        //String[] sids = {"node1.xlab.si", "node2.xlab.si", "node3.xlab.si"};
        String sid = "node1.xlab.si";
        int numOfCores = 2;
        String[] indexes = {"group", "sid", "time"};
        //for (String index : indexes) {
        //    sourceCollection.createIndex(new BasicDBObject(index, 1));
        //}

        boolean loadInterval = false;
        long reminder = (long) (Math.random() * 60);
        int type1 = 1;
        int type2 = 1;

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
            metrics.put("availability", 1);
            commonData.put("metrics", metrics);
            sourceCollection.insert(commonData);

            // cpu metrics
            DBObject cpuData = new BasicDBObject();
            cpuData.put("time", calendar.getTime());
            cpuData.put("group", "cpu");
            cpuData.put("source", "host");
            cpuData.put("sid", sid);
            metrics = new BasicDBObject();
            metrics.put("cores", numOfCores);

            metrics.put("speed", 2600);
            double system = getRndNumDouble(5, 2);
            metrics.put("system", system);

            if (loadInterval) {
                double user;
                if (type1 == 1) {
                    user = getRndNumDouble(90, 5, 2);
                }
                else if (type1 == 2) {
                    user = getRndNumDouble(80, 15, 2);
                }
                else {
                    user = getRndNumDouble(60, 15, 2);
                }

                metrics.put("user", user);
                metrics.put("idle", 100 - system - user);
                metrics.put("load_one", round((user + system) / 100 * numOfCores, 2));
                metrics.put("load_five", round((user + system) / 100 * numOfCores, 2));
            }
            else {
                double user;
                if (type2 == 1) {
                    user = getRndNumDouble(20, 10, 2);
                }
                else if (type2 == 2) {
                    user = getRndNumDouble(35, 15, 2);
                }
                else {
                    user = getRndNumDouble(10, 7, 2);
                }

                metrics.put("user", user);
                metrics.put("idle", 100 - system - user);
                metrics.put("load_one", round((user + system) / 100 * numOfCores, 2));
                metrics.put("load_five", round((user + system) / 100 * numOfCores, 2));
            }

            reminder -= interval;
            if (reminder <= 0) {
                loadInterval = !loadInterval;
                if (loadInterval) {
                    reminder = (long) (300 + Math.random() * 600);
                    double r = Math.random();
                    if (r < 1 / 3.0) {
                        type1 = 1;
                    }
                    else if (r > 2 / 3.0) {
                        type1 = 2;
                    }
                    else {
                        type1 = 3;
                    }
                }
                else {
                    reminder = (long) (600 + Math.random() * 7200);
                    double r = Math.random();
                    if (r < 1 / 3.0) {
                        type2 = 1;
                    }
                    else if (r > 2 / 3.0) {
                        type2 = 2;
                    }
                    else {
                        type2 = 3;
                    }
                }
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
            metrics.put("total", getRndNumInt(16000));
            metrics.put("free", getRndNumInt(12000));
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

        System.out.println("Test data generated successfully.");

        System.out.println("Archiving metrics data...");
        Archiver archiver = new Archiver(mongoClient, db.getName());
        archiver.compressNewData();
        System.out.println("Archiver finished successfully.");
    }

    private void prepareTestData2(MongoClient mongoClient, String databaseName,
                                  Calendar startTime, Calendar stopTime, int interval) throws IOException {

        DB db = mongoClient.getDB(databaseName);
        DBCollection sourceCollection = db.getCollection(Conf.RAW_COLL_NAME);
        DBCollection targetCollection = db.getCollection(Conf.COMPRESSED_COLL_NAME);

        System.out.println(String.format(
                "Generating test metrics data in the database %s on the host %s...", databaseName,
                mongoClient.getAddress().toString()));

        //String[] sids = {"node1.xlab.si", "node2.xlab.si", "node3.xlab.si"};
        String sid = "node2.xlab.si";

        boolean loadInterval = false;
        long reminder = (long) (Math.random() * 60);
        int type1 = 1;
        int type2 = 1;
        int numOfCores = 4;

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
            metrics.put("availability", 1);
            commonData.put("metrics", metrics);
            sourceCollection.insert(commonData);

            // cpu metrics
            DBObject cpuData = new BasicDBObject();
            cpuData.put("time", calendar.getTime());
            cpuData.put("group", "cpu");
            cpuData.put("source", "host");
            cpuData.put("sid", sid);
            metrics = new BasicDBObject();
            metrics.put("cores", numOfCores);

            metrics.put("speed", 2800);
            double system = getRndNumDouble(5, 2);
            metrics.put("system", system);

            if (loadInterval) {
                double user;
                if (type1 == 1) {
                    user = getRndNumDouble(80, 5, 2);
                }
                else if (type1 == 2) {
                    user = getRndNumDouble(60, 15, 2);
                }
                else {
                    user = getRndNumDouble(50, 15, 2);
                }

                metrics.put("user", user);
                metrics.put("idle", 100 - system - user);
                metrics.put("load_one", round((user + system) / 100 * numOfCores, 2));
                metrics.put("load_five", round((user + system) / 100 * numOfCores, 2));
            }
            else {
                double user;
                if (type2 == 1) {
                    user = getRndNumDouble(15, 10, 2);
                }
                else if (type2 == 2) {
                    user = getRndNumDouble(40, 20, 2);
                }
                else {
                    user = getRndNumDouble(5, 5, 2);
                }

                metrics.put("user", user);
                metrics.put("idle", 100 - system - user);
                metrics.put("load_one", round((user + system) / 100 * numOfCores, 2));
                metrics.put("load_five", round((user + system) / 100 * numOfCores, 2));
            }

            reminder -= interval;
            if (reminder <= 0) {
                loadInterval = !loadInterval;
                if (loadInterval) {
                    reminder = (long) (300 + Math.random() * 600);
                    double r = Math.random();
                    if (r < 1 / 3.0) {
                        type1 = 1;
                    }
                    else if (r > 2 / 3.0) {
                        type1 = 2;
                    }
                    else {
                        type1 = 3;
                    }
                }
                else {
                    reminder = (long) (600 + Math.random() * 7200);
                    double r = Math.random();
                    if (r < 1 / 3.0) {
                        type2 = 1;
                    }
                    else if (r > 2 / 3.0) {
                        type2 = 2;
                    }
                    else {
                        type2 = 3;
                    }
                }
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
            metrics.put("total", getRndNumInt(16000));
            metrics.put("free", getRndNumInt(8000));
            metrics.put("used", getRndNumInt(8000));
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


    private double getRndNumDouble(double mean, double diff, int precision) {
        double d = mean + (Math.random() * 2 - 1) * diff;
        BigDecimal bd = new BigDecimal(d).setScale(precision, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }

    private double round(double num, int precision) {
        BigDecimal bd = new BigDecimal(num).setScale(precision, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }
}

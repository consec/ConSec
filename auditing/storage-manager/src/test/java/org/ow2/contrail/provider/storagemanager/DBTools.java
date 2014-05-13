package org.ow2.contrail.provider.storagemanager;

import com.mongodb.*;
import org.junit.Test;
import org.ow2.contrail.provider.storagemanager.common.Metric;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;

import java.io.IOException;
import java.util.*;

public class DBTools {
    private DB db;

    //@Before
    public void setUp() throws IOException {
        Conf.getInstance().load("src/test/resources/storage-manager.cfg");
        MongoDBConnection.init();
        db = MongoDBConnection.getDB(Conf.getInstance().getMongoDatabase());
    }

    //@After
    public void tearDown() throws Exception {
        MongoDBConnection.close();
    }

    @Test
    public void copy() throws IOException {
        DBCollection sourceCollection = db.getCollection(Conf.RAW_COLL_NAME);

        Calendar startTime = new GregorianCalendar(2013, 6, 25, 0, 0, 0);
        Calendar endTime = new GregorianCalendar(2013, 6, 27, 0, 0, 0);

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("source", "host");
        searchQuery.put("sid", "n0001-xc2-xlab-lan");
        searchQuery.put("time", BasicDBObjectBuilder
                .start("$gte", startTime.getTime())
                .add("$lte", endTime.getTime())
                .get());
        BasicDBObject sortBy = new BasicDBObject("time", 1);
        DBCursor cursor = null;

        try {
            cursor = sourceCollection.find(searchQuery).sort(sortBy);
            while (cursor.hasNext()) {
                DBObject o = cursor.next();

                o.put("sid", "n0004-xc2-xlab-lan");
                o.removeField("_id");
                o.removeField("archived");
                Date time = (Date) o.get("time");
                Calendar cal = new GregorianCalendar();
                cal.setTime(time);
                cal.add(Calendar.DAY_OF_YEAR, 6);
                o.put("time", cal.getTime());

                sourceCollection.insert(o);
            }
        }
        finally {
            cursor.close();
        }
    }

    @Test
    public void printData() throws IOException {
        DBCollection sourceCollection = db.getCollection(Conf.RAW_COLL_NAME);

        Calendar startTime = new GregorianCalendar(2013, 7, 1, 0, 0, 0);
        Calendar endTime = new GregorianCalendar(2013, 7, 2, 0, 0, 0);

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("source", "host");
        searchQuery.put("sid", "n0001-xc2-xlab-lan");
        searchQuery.put("time", BasicDBObjectBuilder
                .start("$gte", startTime.getTime())
                .add("$lte", endTime.getTime())
                .get());
        BasicDBObject sortBy = new BasicDBObject("time", 1);
        DBCursor cursor = null;

        try {
            cursor = sourceCollection.find(searchQuery).sort(sortBy);
            while (cursor.hasNext()) {
                DBObject o = cursor.next();
                Date time = (Date) o.get("time");
                Double value = (Double) ((DBObject) o.get("metrics")).get("load_one");
                if (value != null) {
                    System.out.println(time.getTime() + "\t" + value);
                }
            }
        }
        finally {
            cursor.close();
        }
    }

    @Test
    public void sizeUsageComparisonRaw() throws IOException {
        MongoDBConnection.init("mongodb://localhost");
        db = MongoDBConnection.getDB("monitoring-uncompressed");

        GregorianCalendar startTime = new GregorianCalendar(2013, 5, 1, 0, 0, 0);
        int[] TEST_DURATIONS = {1, 10, 60, 120, 300, 720, 1440, 1440 * 2, 1440 * 3, 1440 * 5, 1440 * 7};

        for (int i = 0; i < TEST_DURATIONS.length; i++) {
            int duration = TEST_DURATIONS[i];
            GregorianCalendar endTime = (GregorianCalendar) startTime.clone();
            endTime.add(Calendar.MINUTE, duration);

            DBCollection sourceCollection = db.getCollection("rawData");
            DBCollection destCollection = db.getCollection("test");
            destCollection.drop();

            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("sid", "host001.test.com");
            searchQuery.put("group", "cpu");
            searchQuery.put("time", BasicDBObjectBuilder
                    .start("$gte", startTime.getTime())
                    .add("$lte", endTime.getTime())
                    .get());

            DBCursor cursor = null;

            try {
                cursor = sourceCollection.find(searchQuery);
                while (cursor.hasNext()) {
                    DBObject o = cursor.next();
                    destCollection.insert(o);
                }
            }
            finally {
                cursor.close();
            }

            CommandResult result = destCollection.getStats();
            System.out.println(duration + "\t" + result.get("size"));
        }
    }

    @Test
    public void sizeUsageComparisonCompressed() throws IOException {
        MongoDBConnection.init("mongodb://localhost");
        db = MongoDBConnection.getDB("monitoring");

        GregorianCalendar startTime = new GregorianCalendar(2013, 5, 1, 0, 0, 0);
        int[] TEST_DURATIONS = {1, 10, 60, 120, 300, 720, 1440, 1440 * 2, 1440 * 3, 1440 * 5, 1440 * 7};

        for (int i = 0; i < TEST_DURATIONS.length; i++) {
            int duration = TEST_DURATIONS[i];
            GregorianCalendar endTime = (GregorianCalendar) startTime.clone();
            endTime.add(Calendar.MINUTE, duration);

            DBCollection sourceCollection = db.getCollection("compressedData");
            DBCollection destCollection = db.getCollection("test");
            destCollection.drop();

            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("sid", "host001.test.com");
            searchQuery.put("group", "cpu");
            searchQuery.put("startTime", BasicDBObjectBuilder
                    .start("$gte", startTime.getTime())
                    .add("$lt", endTime.getTime())
                    .get());

            DBCursor cursor = null;

            try {
                cursor = sourceCollection.find(searchQuery);
                while (cursor.hasNext()) {
                    DBObject o = cursor.next();
                    destCollection.insert(o);
                }
            }
            finally {
                cursor.close();
            }

            CommandResult result = destCollection.getStats();
            System.out.println(duration + "\t" + result.get("size"));
        }
    }

    @Test
    public void getCondensedData() throws Exception {

        HistoryRetriever historyRetriever = new HistoryRetriever(db);

        Calendar startTime = new GregorianCalendar(2013, 7, 1, 0, 0, 0);
        Calendar endTime = new GregorianCalendar(2013, 7, 2, 0, 0, 0);

        List<Metric> metrics = new ArrayList<Metric>();
        Metric metric1 = new Metric("cpu", "load_one");
        metrics.add(metric1);

        List<String> sids = new ArrayList<String>();
        String sid1 = "n0001-xc2-xlab-lan";
        sids.add(sid1);

        CondensedHistoryData history = historyRetriever.getCondensedHistory(metrics,
                "host", sids, startTime.getTime(), endTime.getTime(), 96);

        for (int i = 0; i < history.getNumberOfIntervals(); i++) {
            CondensedDataValue value = history.getMetricsData(sid1).getMetricData(metric1)[i];
            Date time = history.getIntervals()[i];
            double timeExcel = ((time.getTime() + 7200000) / 86400000.0) + 25569;
            System.out.println(timeExcel + "\t" + value.getMin() + "\t" + value.getAvg() + "\t" + value.getMax());
        }
    }
}

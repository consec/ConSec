package org.ow2.contrail.provider.storagemanager;

import com.mongodb.*;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ow2.contrail.provider.storagemanager.common.Metric;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;

@Ignore // local MongoDB instance is needed
public class ArchiverTest {
    private static Logger log = Logger.getLogger(ArchiverTest.class);
    private MongoClient mongoClient;
    private DB db;

    @Before
    public void setUp() throws IOException {
        Conf.getInstance().load("src/test/resources/storage-manager.cfg");
        MongoDBConnection.init();
        mongoClient = MongoDBConnection.getMongoClient();
        db = MongoDBConnection.getDB(Conf.getInstance().getMongoDatabase());
    }

    @After
    public void tearDown() throws Exception {
        MongoDBConnection.close();
    }

    @Test
    public void testCompression() throws Exception {
        DBCollection sourceCollection = db.getCollection(Conf.RAW_COLL_NAME);
        DBCollection targetCollection = db.getCollection(Conf.COMPRESSED_COLL_NAME);
        sourceCollection.drop();
        targetCollection.drop();

        Archiver archiver = new Archiver(mongoClient, db.getName());
        HistoryRetriever historyRetriever = new HistoryRetriever(db);

        Calendar startTime = Calendar.getInstance();
        startTime.set(2012, 5, 1, 0, 0, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        Calendar endTime = Calendar.getInstance();
        endTime.set(2012, 5, 3, 23, 59, 59);
        endTime.set(Calendar.MILLISECOND, 0);

        try {
            Utils.prepareTestData(sourceCollection, startTime, endTime, 15);
            double rawDataSize = (Double) db.eval(
                    String.format("db.%s.dataSize()", Conf.RAW_COLL_NAME));

            Date t1 = new Date();
            archiver.compressNewData();
            Date t2 = new Date();
            long dt = t2.getTime() - t1.getTime();

            double compressedDataSize = (Double) db.eval(
                    String.format("db.%s.dataSize()", Conf.COMPRESSED_COLL_NAME));
            double ratio = compressedDataSize / rawDataSize;

            System.out.println(String.format("Raw data size: %.2f kB", rawDataSize / 1024));
            System.out.println(String.format("Compressed data size: %.2f kB", compressedDataSize / 1024));
            System.out.println(String.format("Ratio: %.3f", ratio));
            System.out.println(String.format("Compression time: %.3f s", dt / 1000.0));

            Calendar from = Calendar.getInstance();
            from.set(Calendar.MILLISECOND, 0);
            Calendar to = Calendar.getInstance();
            to.set(Calendar.MILLISECOND, 0);

            from.set(2012, 5, 1, 1, 10, 0);
            to.set(2012, 5, 1, 1, 11, 0);
            compareDecompressedData(new Metric("cpu", "speed"), "host", "host001.test.com", from, to,
                    historyRetriever, sourceCollection);

            from.set(2012, 5, 1, 12, 0, 0);
            to.set(2012, 5, 2, 12, 0, 0);
            compareDecompressedData(new Metric("cpu", "speed"), "host", "host001.test.com", from, to,
                    historyRetriever, sourceCollection);

            from.set(2012, 5, 1, 0, 0, 0);
            to.set(2012, 5, 3, 23, 59, 59);
            compareDecompressedData(new Metric("cpu", "speed"), "host", "host001.test.com", from, to,
                    historyRetriever, sourceCollection);

            from.set(2012, 5, 1, 8, 0, 0);
            to.set(2012, 5, 1, 9, 0, 0);
            compareDecompressedData(new Metric("common", "hostname"), "host", "host001.test.com", from, to,
                    historyRetriever, sourceCollection);

            from.set(2012, 5, 1, 1, 10, 0);
            to.set(2012, 5, 1, 1, 11, 0);
            List<Metric> metrics = new ArrayList<Metric>();
            metrics.add(new Metric("cpu", "cores"));
            metrics.add(new Metric("cpu", "speed"));
            metrics.add(new Metric("cpu", "idle"));
            MetricsHistoryData history = historyRetriever.getHistory(
                    metrics, "host", "host001.test.com", startTime.getTime(), endTime.getTime());
            assertEquals(history.getTimeValues().size(), 17280);
            assertEquals(history.getMetricsData().size(), 3);
            assertEquals(history.getMetricValues("cpu.idle").size(), 17280);
            assertEquals(history.getMetricValues("cpu.speed").size(), 17280);
            assertEquals(history.getMetricValues("cpu.cores").size(), 17280);
        }
        finally {
            sourceCollection.drop();
            targetCollection.drop();
        }
    }

    private void compareDecompressedData(Metric metric, String source, String sid, Calendar startTime,
                                         Calendar endTime, HistoryRetriever historyRetriever, DBCollection sourceCollection) throws Exception {
        MetricsHistoryData history = historyRetriever.getHistory(metric, source, sid,
                startTime.getTime(), endTime.getTime());

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("group", metric.getGroup());
        searchQuery.put("source", source);
        searchQuery.put("sid", sid);
        searchQuery.put("time", BasicDBObjectBuilder
                .start("$gte", startTime.getTime())
                .add("$lte", endTime.getTime())
                .get());
        BasicDBObject sortBy = new BasicDBObject("time", 1);
        DBCursor cursor = null;

        try {
            cursor = sourceCollection.find(searchQuery).sort(sortBy);
            assertEquals(cursor.size(), history.getMetricValues(metric).size());
            int i = 0;
            while (cursor.hasNext()) {
                DBObject record = cursor.next();
                DBObject metrics = (DBObject) record.get("metrics");
                Object value = metrics.get(metric.getName());
                assertEquals(history.getTimeValues().get(i), record.get("time"));
                assertEquals(history.getMetricValues(metric).get(i), value);
                i++;
            }
        }
        finally {
            if (cursor != null)
                cursor.close();
        }
    }

    @Test
    public void testCompressedRawDataJoin() throws Exception {
        DBCollection sourceCollection = db.getCollection(Conf.RAW_COLL_NAME);
        DBCollection targetCollection = db.getCollection(Conf.COMPRESSED_COLL_NAME);
        sourceCollection.drop();
        targetCollection.drop();

        Archiver archiver = new Archiver(mongoClient, db.getName());
        HistoryRetriever historyRetriever = new HistoryRetriever(db);

        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, 23);
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);
        startTime.add(Calendar.DAY_OF_YEAR, -1);

        Calendar endTime = (Calendar) startTime.clone();
        endTime.add(Calendar.HOUR, 1);
        endTime.add(Calendar.MINUTE, 30);

        try {
            Utils.prepareTestData(sourceCollection, startTime, endTime, 15);

            double sourceCollCount = (Double) db.eval(
                    String.format("db.%s.count()", Conf.RAW_COLL_NAME));
            assertEquals(sourceCollCount, 1800.0);

            archiver.compressNewData();

            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put("archived", new BasicDBObject("$exists", false));
            DBCursor cursor = sourceCollection.find(searchQuery);
            assertEquals(cursor.size(), 600);

            searchQuery = new BasicDBObject();
            searchQuery.put("archived", true);
            cursor = sourceCollection.find(searchQuery);
            assertEquals(cursor.size(), 1200);

            double targetCollCount = (Double) db.eval(
                    String.format("db.%s.count()", Conf.COMPRESSED_COLL_NAME));
            assertEquals(targetCollCount, 5.0);

            Metric metric = new Metric("cpu", "load_one");
            MetricsHistoryData history = historyRetriever.getHistory(metric, "host", "host001.test.com",
                    startTime.getTime(), endTime.getTime());
            assertEquals(history.getTimeValues().size(), 360);
            assertEquals(history.getMetricsData().size(), 1);
            assertEquals(history.getMetricValues(metric).size(), 360);
        }
        finally {
            sourceCollection.drop();
            targetCollection.drop();
        }
    }

    @Test
    public void testGzipCompression() throws IOException {
        String text = "12,452,103,771,4,29,194,320,625";
        byte[] bytes = Archiver.compressGzip(text);
        String decompressed = Archiver.decompressGzip(bytes);
        assertEquals(decompressed, text);
    }
}

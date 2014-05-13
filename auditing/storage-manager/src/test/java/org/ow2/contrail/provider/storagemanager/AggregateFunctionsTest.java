package org.ow2.contrail.provider.storagemanager;

import com.mongodb.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ow2.contrail.provider.storagemanager.aggregates.MaxAggregate;
import org.ow2.contrail.provider.storagemanager.aggregates.MeanAggregate;
import org.ow2.contrail.provider.storagemanager.aggregates.MinAggregate;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;

import java.io.IOException;
import java.util.Calendar;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore // local MongoDB instance is needed
public class AggregateFunctionsTest {
    private DBCollection sourceCollection;
    private DBCollection targetCollection;
    private Archiver archiver;
    private DataRetriever dataRetriever;

    @Before
    public void setUp() throws IOException {
        Conf.getInstance().load("src/test/resources/storage-manager.cfg");
        MongoDBConnection.init();
        DB db = MongoDBConnection.getDB(Conf.getInstance().getMongoDatabase());
        sourceCollection = db.getCollection(Conf.RAW_COLL_NAME);
        targetCollection = db.getCollection(Conf.COMPRESSED_COLL_NAME);

        archiver = new Archiver();
        dataRetriever = new DataRetriever(db);
    }

    @After
    public void tearDown() throws Exception {
        MongoDBConnection.close();
    }

    @Test
    public void testAggregates() throws Exception {
        targetCollection.drop();

        Calendar startTime = Calendar.getInstance();
        startTime.set(2012, 5, 1, 10, 0, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        Calendar endTime = (Calendar) startTime.clone();
        endTime.add(Calendar.MINUTE, 7);

        prepareTestData(startTime);
        archiver.compressNewData();

        MaxAggregate maxAggregate = new MaxAggregate("load_one");
        dataRetriever.calculateAggregateValue("cpu", "load_one", "host", "host001.test.com",
                startTime.getTime(), endTime.getTime(), maxAggregate);
        Map<String, Object> max = maxAggregate.getAggregateValue();
        assertEquals(max.get("value"), 0.72);

        MinAggregate minAggregate = new MinAggregate("load_one");
        dataRetriever.calculateAggregateValue("cpu", "load_one", "host", "host001.test.com",
                startTime.getTime(), endTime.getTime(), minAggregate);
        Map<String, Object> min = minAggregate.getAggregateValue();
        assertEquals(min.get("value"), 0.10);

        MeanAggregate meanAggregate = new MeanAggregate("load_one");
        dataRetriever.calculateAggregateValue("cpu", "load_one", "host", "host001.test.com",
                startTime.getTime(), endTime.getTime(), meanAggregate);
        Map<String, Object> mean = meanAggregate.getAggregateValue();
        assertEquals(mean.get("mean"), 0.31125);
        assertTrue(Math.abs((Double) mean.get("stddev") - 0.197511867) < 1E-9);
    }

    private void prepareTestData(Calendar startTime) {
        sourceCollection.drop();

        double[] load_one_values = new double[]{0.27, 0.55, 0.72, 0.20, 0.25, 0.22, 0.18, 0.10};
        String sid = "host001.test.com";

        Calendar time = (Calendar) startTime.clone();
        for (double load_one : load_one_values) {
            DBObject metrics;

            // cpu metrics
            DBObject cpuData = new BasicDBObject();
            cpuData.put("time", time.getTime());
            cpuData.put("group", "cpu");
            cpuData.put("source", "host");
            cpuData.put("sid", sid);
            metrics = new BasicDBObject();
            metrics.put("cores", 8);
            metrics.put("speed", 3000);
            metrics.put("user", 20.15);
            metrics.put("system", 5.68);
            metrics.put("idle", 74.17);
            metrics.put("load_one", load_one);
            metrics.put("load_five", 0.50);
            cpuData.put("metrics", metrics);
            sourceCollection.insert(cpuData);

            time.add(Calendar.SECOND, 60);
        }
    }

}

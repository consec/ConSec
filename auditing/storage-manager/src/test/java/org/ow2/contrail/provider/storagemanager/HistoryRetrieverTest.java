package org.ow2.contrail.provider.storagemanager;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ow2.contrail.provider.storagemanager.common.Metric;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Ignore // local MongoDB instance is needed
public class HistoryRetrieverTest {
    private MongoClient mongoClient;
    private DB db;

    @Before
    public void setUp() throws IOException {
        Conf.getInstance().load("src/test/resources/storage-manager.cfg");
        MongoDBConnection.init();
        mongoClient = MongoDBConnection.getMongoClient();
        db = mongoClient.getDB(Conf.getInstance().getMongoDatabase());
    }

    @After
    public void tearDown() throws Exception {
        mongoClient.close();
    }

    @Test
    public void testGetHistory() throws Exception {

        HistoryRetriever historyRetriever = new HistoryRetriever(db);

        Calendar startTime = Calendar.getInstance();
        startTime.set(2013, 5, 1, 0, 0, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        Calendar endTime = Calendar.getInstance();
        endTime.set(2013, 5, 1, 23, 59, 59);
        endTime.set(Calendar.MILLISECOND, 0);

        List<Metric> metrics = new ArrayList<Metric>();
        Metric metric1 = new Metric("cpu", "load_one");
        metrics.add(metric1);
        Metric metric2 = new Metric("cpu", "load_five");
        metrics.add(metric2);
        Metric metric3 = new Metric("cpu", "idle");
        metrics.add(metric3);

        MetricsHistoryData history = historyRetriever.getHistory(metrics,
                "host", "host001.test.com", startTime.getTime(), endTime.getTime());

        assertEquals(history.size(), 5760);
        assertEquals(history.getTimeValues().size(), 5760);
        assertEquals(history.getMetricsData().size(), 3);
        assertEquals(history.getMetricValues(metric1).size(), 5760);
        assertEquals(history.getMetricValues(metric2).size(), 5760);
        assertEquals(history.getMetricValues(metric3).size(), 5760);
    }

    @Test
    public void testGetCondensedHistory() throws Exception {

        HistoryRetriever historyRetriever = new HistoryRetriever(db);

        Calendar startTime = new GregorianCalendar(2013, 5, 1, 0, 0, 0);
        Calendar endTime = new GregorianCalendar(2013, 5, 1, 1, 0, 0);

        List<Metric> metrics = new ArrayList<Metric>();
        Metric metric1 = new Metric("cpu", "load_one");
        metrics.add(metric1);
        Metric metric2 = new Metric("cpu", "cores");
        metrics.add(metric2);
        Metric metric3 = new Metric("memory", "used");
        metrics.add(metric3);

        List<String> sids = new ArrayList<String>();
        sids.add("host001.test.com");

        CondensedHistoryData history = historyRetriever.getCondensedHistory(metrics,
                "host", sids, startTime.getTime(), endTime.getTime(), 60);

        assertEquals(history.getIntervals().length, 60);
        assertEquals(history.getStartTime(), startTime.getTime());
        assertEquals(history.getEndTime(), endTime.getTime());
        assertEquals(history.getIntervalLength(), 60000);

        CondensedHistoryData.MetricsData metricsData = history.getMetricsData(sids.get(0));
        assertEquals(metricsData.getMetricData(metric1).length, 60);
        assertEquals(metricsData.getMetricData(metric2).length, 60);
        assertEquals(metricsData.getMetricData(metric3).length, 60);

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(history);
        System.out.println(json);
    }
}

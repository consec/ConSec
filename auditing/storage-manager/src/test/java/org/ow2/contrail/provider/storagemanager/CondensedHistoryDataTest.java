package org.ow2.contrail.provider.storagemanager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ow2.contrail.provider.storagemanager.common.Metric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@Ignore
public class CondensedHistoryDataTest {

    @Before
    public void setUp() throws IOException {
        Conf.getInstance().load("src/test/resources/storage-manager.cfg");
    }

    @Test
    public void testAddData() {
        Calendar startTime = new GregorianCalendar(2012, 5, 1, 9, 0, 0);
        Calendar endTime = new GregorianCalendar(2012, 5, 1, 10, 0, 0);
        int numberOfIntervals = 12;

        List<Metric> metrics = new ArrayList<Metric>();
        Metric metric1 = new Metric("cpu", "load_one");
        metrics.add(metric1);
        Metric metric2 = new Metric("cpu", "cores");
        metrics.add(metric2);

        List<String> sids = new ArrayList<String>();
        sids.add("host001.test.com");

        CondensedHistoryData historyData = new CondensedHistoryData(sids, metrics, startTime.getTime(),
                endTime.getTime(), numberOfIntervals);

        // check intervals
        assertEquals(historyData.getIntervals().length, numberOfIntervals);
        assertEquals(historyData.getIntervals()[0], new GregorianCalendar(2012, 5, 1, 9, 2, 30).getTime());
        assertEquals(historyData.getIntervals()[1], new GregorianCalendar(2012, 5, 1, 9, 7, 30).getTime());

        HistoryRetriever.CondensedHistoryBuffer buffer =
                new HistoryRetriever.CondensedHistoryBuffer(metrics, historyData);

        // add metric1 values
        buffer.addMetricValue(metric1, 0.50, new GregorianCalendar(2012, 5, 1, 9, 0, 30).getTime());
        buffer.addMetricValue(metric1, 1.50, new GregorianCalendar(2012, 5, 1, 9, 1, 25).getTime());
        buffer.addMetricValue(metric1, 1.60, new GregorianCalendar(2012, 5, 1, 9, 2, 32).getTime());
        buffer.addMetricValue(metric1, 2.20, new GregorianCalendar(2012, 5, 1, 9, 5, 10).getTime());
        buffer.addMetricValue(metric1, 2.60, new GregorianCalendar(2012, 5, 1, 9, 6, 25).getTime());
        buffer.addMetricValue(metric1, 1.30, new GregorianCalendar(2012, 5, 1, 9, 12, 5).getTime());
        buffer.addMetricValue(metric1, 3.00, new GregorianCalendar(2012, 5, 1, 9, 20, 15).getTime());
        buffer.addMetricValue(metric1, 3.40, new GregorianCalendar(2012, 5, 1, 9, 21, 30).getTime());

        // add metric2 values
        buffer.addMetricValue(metric2, 2, new GregorianCalendar(2012, 5, 1, 9, 5, 30).getTime());
        buffer.addMetricValue(metric2, 2, new GregorianCalendar(2012, 5, 1, 9, 6, 25).getTime());
        buffer.addMetricValue(metric2, 4, new GregorianCalendar(2012, 5, 1, 9, 8, 32).getTime());

        // finalize
        buffer.finish();

        // check metric 1 values
        double delta = 1E-9;
        CondensedDataValue[] metric1Values = buffer.getMetricValues(metric1);
        assertEquals(metric1Values.length, 12);
        CondensedDataValue value10 = metric1Values[0];
        assertEquals(value10.getMin(), 0.50, delta);
        assertEquals(value10.getMax(), 1.60, delta);
        assertEquals(value10.getAvg(), 1.20, delta);

        assertNull(metric1Values[3]);

        CondensedDataValue value14 = metric1Values[4];
        assertEquals(value14.getMin(), 3.00, delta);
        assertEquals(value14.getMax(), 3.40, delta);
        assertEquals(value14.getAvg(), 3.20, delta);

        // check metric 2 values
        CondensedDataValue[] metric2Values = buffer.getMetricValues(metric2);
        assertEquals(metric2Values.length, 12);

        assertNull(metric2Values[0]);

        CondensedDataValue value21 = metric2Values[1];
        assertEquals(value21.getMin(), 2.0, delta);
        assertEquals(value21.getMax(), 4.0, delta);
        assertEquals(value21.getAvg(), 8.0/3, delta);
    }
}

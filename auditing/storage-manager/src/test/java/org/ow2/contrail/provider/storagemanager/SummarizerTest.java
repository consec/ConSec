package org.ow2.contrail.provider.storagemanager;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SummarizerTest {
    @Test
    public void createEnumSummary1() {
        String metric = "availability";
        Object[] values = new Integer[]{1, 1, 1};
        List<DBObject> history = prepareTestData(metric, values);
        DBObject summary = Summarizer.createSummary(metric, history);
        System.out.println(summary);
    }

    @Test
    public void createEnumSummary2() {
        String metric = "availability";
        Object[] values = new Integer[]{1, 1, 0};
        List<DBObject> history = prepareTestData(metric, values);
        DBObject summary = Summarizer.createSummary(metric, history);
        System.out.println(summary);
    }

    @Test
    public void createEnumSummary3() {
        String metric = "availability";
        Object[] values = new Integer[]{1, 1, 1, 1, 0, 0, 0, 1, 0};
        List<DBObject> history = prepareTestData(metric, values);
        DBObject summary = Summarizer.createSummary(metric, history);
        System.out.println(summary);
    }

    private List<DBObject> prepareTestData(String metric, Object[] values) {
        List<DBObject> history = new ArrayList<DBObject>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2012, 5, 1, 12, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        for (Object value : values) {
            DBObject historyItem = new BasicDBObject();
            historyItem.put("time", calendar.getTime());
            DBObject metrics = new BasicDBObject();
            metrics.put(metric, value);
            historyItem.put("metrics", metrics);
            history.add(historyItem);
            calendar.add(Calendar.SECOND, 10);
        }
        return history;
    }
}

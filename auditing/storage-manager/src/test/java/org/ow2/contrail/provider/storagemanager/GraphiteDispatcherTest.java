package org.ow2.contrail.provider.storagemanager;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Scanner;

@Ignore
public class GraphiteDispatcherTest {

    @Test
    public void testSendMetricsData() throws Exception {
        Scanner s = new Scanner(new File("src/test/resources/metrics-data.json")).useDelimiter("\\Z");
        String json = s.next();
        s.close();
        BasicDBObject record = (BasicDBObject) JSON.parse(json);
        GraphiteDispatcher dispatcher = new GraphiteDispatcher("172.16.118.158", 2003);
        dispatcher.sendMetricsData(record);
    }
}

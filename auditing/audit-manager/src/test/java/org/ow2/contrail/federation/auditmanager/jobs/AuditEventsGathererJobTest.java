package org.ow2.contrail.federation.auditmanager.jobs;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ow2.contrail.federation.auditmanager.utils.Conf;
import org.ow2.contrail.federation.auditmanager.utils.MongoDBConnection;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class AuditEventsGathererJobTest {
    @Before
    public void setUp() throws Exception {
        Conf.getInstance().load("src/test/resources/audit-manager.cfg");
        MongoDBConnection.init();
    }

    @After
    public void tearDown() throws Exception {
        MongoDBConnection.close();
    }

    @Test
    public void test() throws Exception {
        JSONObject searchCriteria = new JSONObject();
        searchCriteria.put("outcome", "success");
        Calendar startTime = new GregorianCalendar(2013, 8, 27, 10, 0, 0);
        Calendar endTime = new GregorianCalendar(2013, 8, 27, 11, 0, 0);
        AuditEventsGathererJob job = new AuditEventsGathererJob(searchCriteria, startTime.getTime(), endTime.getTime());
        Thread t = new Thread(job);
        t.start();
        t.join();
    }
}

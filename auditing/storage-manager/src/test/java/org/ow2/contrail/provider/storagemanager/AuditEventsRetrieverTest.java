package org.ow2.contrail.provider.storagemanager;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;

import java.io.IOException;
import java.util.Calendar;

@Ignore // local MongoDB instance is needed
public class AuditEventsRetrieverTest {
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

        AuditEventsRetriever auditEventsRetriever = new AuditEventsRetriever(db);

        Calendar startTime = Calendar.getInstance();
        startTime.set(2013, 8, 24, 15, 30, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        Calendar endTime = Calendar.getInstance();
        endTime.set(2013, 8, 24, 15, 34, 0);
        endTime.set(Calendar.MILLISECOND, 0);

        JSONObject searchQuery = new JSONObject();
        searchQuery.put("action", "create");
        searchQuery.put("outcome", "SUCCESS");
        searchQuery.put("initiator.oauthAccessToken", "0d0089fd-78b7-3e75-b4bd-eaeca9530ce6");

        String json = auditEventsRetriever.find(searchQuery.toString(), startTime.getTime(), endTime.getTime(), 0, 3);
        System.out.println(json);
    }
}

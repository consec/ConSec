package org.ow2.contrail.provider.storagemanager;

import com.mongodb.DB;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;

import java.io.IOException;

@Ignore // local MongoDB instance is needed
public class DataRetrieverTest {
    private DataRetriever dataRetriever;

    @Before
    public void setUp() throws IOException {
        Conf.getInstance().load("src/test/resources/storage-manager.cfg");
        DB db = MongoDBConnection.getDB(Conf.getInstance().getMongoDatabase());
        dataRetriever = new DataRetriever(db);
    }

    @Test
    public void testGetEvents() throws Exception {
        JSONArray events = dataRetriever.getEvents("testApp", "testUser");
        System.out.println(events.toString());
    }
}

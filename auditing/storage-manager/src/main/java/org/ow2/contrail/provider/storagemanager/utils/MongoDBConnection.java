package org.ow2.contrail.provider.storagemanager.utils;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.apache.log4j.Logger;
import org.ow2.contrail.provider.storagemanager.Conf;

import java.net.UnknownHostException;

public class MongoDBConnection {
    private static Logger log = Logger.getLogger(MongoDBConnection.class);
    private static MongoClient mongoClient;

    public static void init() throws UnknownHostException {
        String connectionString = Conf.getInstance().getMongoConnectionString();
        connect(connectionString);
    }

    public static void init(String connectionString) throws UnknownHostException {
        connect(connectionString);
    }

    private static void connect(String connectionString) throws UnknownHostException {
        log.debug("Connecting to MongoDB using connection string " + connectionString);
        MongoClientURI uri = new MongoClientURI(connectionString);
        mongoClient = new MongoClient(uri);
        log.info("Connection to MongoDB established successfully.");
    }

    public static void close() {
        mongoClient.close();
        log.info("Connection to MongoDB closed.");
    }

    public static MongoClient getMongoClient() {
        return mongoClient;
    }

    public static DB getDB(String dbName) {
        return mongoClient.getDB(dbName);
    }
}

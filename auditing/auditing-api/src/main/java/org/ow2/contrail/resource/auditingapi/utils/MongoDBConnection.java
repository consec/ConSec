package org.ow2.contrail.resource.auditingapi.utils;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.apache.log4j.Logger;

import java.net.UnknownHostException;

public class MongoDBConnection {
    private static Logger log = Logger.getLogger(MongoDBConnection.class);
    private static MongoClient mongoClient;
    private static DB db;

    public static void init() throws UnknownHostException {
        String connectionString = Conf.getInstance().getMongoDBConnectionString();
        String dbName = Conf.getInstance().getMongoDBDatabase();
        connect(connectionString, dbName);
    }

    public static void init(String connectionString, String dbName) throws UnknownHostException {
        connect(connectionString, dbName);
    }

    private static void connect(String connectionString, String dbName) throws UnknownHostException {
        log.debug("Connecting to MongoDB...");
        MongoClientURI uri = new MongoClientURI(connectionString);
        mongoClient = new MongoClient(uri);
        db = mongoClient.getDB(dbName);
        log.debug("Connection to MongoDB established successfully.");
    }

    public static void close() {
        mongoClient.close();
        log.debug("Connection to MongoDB closed.");
    }

    public static DB getDB() {
        return db;
    }
}

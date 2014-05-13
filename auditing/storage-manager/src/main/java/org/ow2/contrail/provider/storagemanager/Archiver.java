package org.ow2.contrail.provider.storagemanager;

import com.mongodb.*;
import org.apache.log4j.Logger;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Archiver implements Job {
    private static Logger log = Logger.getLogger(Archiver.class);
    private int numOfArchivedRecord;
    private MongoClient mongoClient;
    private DBCollection sourceCollection;
    private DBCollection targetCollection;

    public Archiver() throws UnknownHostException {
        mongoClient = MongoDBConnection.getMongoClient();
        DB db = MongoDBConnection.getDB(Conf.getInstance().getMongoDatabase());
        sourceCollection = db.getCollection(Conf.RAW_COLL_NAME);
        targetCollection = db.getCollection(Conf.COMPRESSED_COLL_NAME);
    }

    public Archiver(MongoClient mongoClient, String databaseName) throws UnknownHostException {
        this.mongoClient = mongoClient;

        DB db = mongoClient.getDB(databaseName);
        sourceCollection = db.getCollection(Conf.RAW_COLL_NAME);
        targetCollection = db.getCollection(Conf.COMPRESSED_COLL_NAME);
    }

    public void close() {
        try {
            mongoClient.close();
        }
        catch (Exception ignored) {
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.trace("execute() started by the scheduler.");

        try {
            compressNewData();
            log.trace("execute() completed successfully.");
        }
        catch (IOException e) {
            log.error("Failed to archive monitoring data: " + e.getMessage(), e);
            throw new JobExecutionException("Failed to archive monitoring data: " + e.getMessage(), e);
        }
    }

    public void compressNewData() throws IOException {
        log.info("Archiver started. Compressing new monitoring data...");
        Calendar stopTime = Calendar.getInstance();
        stopTime.set(Calendar.HOUR_OF_DAY, 0);
        stopTime.set(Calendar.MINUTE, 0);
        stopTime.set(Calendar.SECOND, 0);
        stopTime.set(Calendar.MILLISECOND, 0);

        @SuppressWarnings("unchecked")
        List<String> groups = (List<String>) sourceCollection.distinct("group");
        @SuppressWarnings("unchecked")
        List<String> sids = (List<String>) sourceCollection.distinct("sid");
        String source = "host";

        numOfArchivedRecord = 0;
        Date t1 = new Date();
        for (String group : groups) {
            for (String sid : sids) {
                compressMetricsGroupHistory(group, source, sid, stopTime);
            }
        }
        Date t2 = new Date();
        long duration = t2.getTime() - t1.getTime();
        log.trace("Compressing of monitoring data finished successfully.");
        log.trace("Number of records archived: " + numOfArchivedRecord);
        log.trace(String.format("Elapsed time: %.2f s", duration / 1000.0));
        log.info("Archiver completed successfully.");
    }

    private void compressMetricsGroupHistory(String group, String source, String sid, Calendar stopTime)
            throws IOException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Compressing metrics group ('%s', '%s', '%s') monitoring data...",
                    source, sid, group));
        }
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("group", group);
        searchQuery.put("sid", sid);
        searchQuery.put("archived", new BasicDBObject("$exists", false));
        BasicDBObject sortBy = new BasicDBObject("time", 1);
        DBCursor cursor = null;

        try {
            cursor = sourceCollection.find(searchQuery).sort(sortBy);
            log.trace("Number of records found: " + cursor.size());

            boolean isFirstInterval = true;
            DBObject record = null;
            while (cursor.hasNext()) {
                List<Object> archivedOIDs = new ArrayList<Object>();
                DBObject firstRecord;
                if (isFirstInterval) {
                    firstRecord = cursor.next();
                    isFirstInterval = false;
                }
                else {
                    firstRecord = record;
                }

                Calendar startTime = Calendar.getInstance();
                startTime.setTime((Date) firstRecord.get("time"));
                startTime.set(Calendar.HOUR_OF_DAY, 0);
                startTime.set(Calendar.MINUTE, 0);
                startTime.set(Calendar.SECOND, 0);
                startTime.set(Calendar.MILLISECOND, 0);

                Calendar endTime = (Calendar) startTime.clone();
                endTime.add(Calendar.DAY_OF_MONTH, 1);

                if (endTime.after(stopTime)) {
                    break;
                }

                BasicDBObject metricsData = new BasicDBObject();
                BasicDBList timestampList = new BasicDBList();
                metricsData.put("time", timestampList);
                addRecord(firstRecord, metricsData, timestampList);
                archivedOIDs.add(firstRecord.get("_id"));

                while (cursor.hasNext()) {
                    record = cursor.next();

                    Calendar recordCal = Calendar.getInstance();
                    recordCal.setTime((Date) record.get("time"));
                    if (recordCal.get(Calendar.DAY_OF_YEAR) != startTime.get(Calendar.DAY_OF_YEAR) ||
                            recordCal.get(Calendar.YEAR) != startTime.get(Calendar.YEAR)) {
                        break;
                    }

                    addRecord(record, metricsData, timestampList);
                    archivedOIDs.add(record.get("_id"));
                }

                DBObject compressedRecord = new BasicDBObject();
                compressedRecord.put("startTime", startTime.getTime());
                compressedRecord.put("endTime", endTime.getTime());
                compressedRecord.put("group", group);
                compressedRecord.put("source", source);
                compressedRecord.put("sid", sid);
                byte[] data = compressGzip(metricsData.toString());
                compressedRecord.put("data", data);

                targetCollection.insert(compressedRecord);
                if (log.isTraceEnabled()) {
                    log.trace(String.format("Compressed %d records from %s to %s.",
                            timestampList.size(), startTime.getTime(), endTime.getTime()));
                }

                if (Conf.getInstance().getDeleteArchivedData()) {
                    // remove original data that was archived
                    BasicDBObject deleteQuery = new BasicDBObject();
                    deleteQuery.put("_id", new BasicDBObject("$in", archivedOIDs));
                    sourceCollection.remove(deleteQuery);
                    log.trace("Removed original data.");
                }
                else {
                    // mark original data that was compressed with 'archived' flag
                    BasicDBObject updateCommand = new BasicDBObject();
                    updateCommand.put("$set", new BasicDBObject("archived", true));
                    BasicDBObject updateQuery = new BasicDBObject();
                    updateQuery.put("_id", new BasicDBObject("$in", archivedOIDs));
                    sourceCollection.updateMulti(updateQuery, updateCommand);
                    log.trace("Marked original data as archived.");
                }

                numOfArchivedRecord += archivedOIDs.size();
            }
        }
        finally {
            if (cursor != null)
                cursor.close();
        }
    }

    private void addRecord(DBObject record, BasicDBObject metricsData, BasicDBList timestampList) {
        Set<String> metrics = ((BasicDBObject) record.get("metrics")).keySet();
        for (String metric : metrics) {
            if (!metricsData.containsField(metric)) {
                metricsData.put(metric, new BasicDBList());
            }
            Object value = ((BasicDBObject) record.get("metrics")).get(metric);
            BasicDBList valuesList = (BasicDBList) metricsData.get(metric);
            valuesList.add(value);
        }
        Date timestamp = (Date) record.get("time");
        timestampList.add(timestamp.getTime());
    }

    public static byte[] compressGzip(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(text.getBytes());
        gzip.close();
        return out.toByteArray();
    }

    public static String decompressGzip(byte[] data) throws IOException {
        InputStream bais = new ByteArrayInputStream(data);
        GZIPInputStream gs = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int numBytesRead;
        byte[] tempBytes = new byte[1024];
        while ((numBytesRead = gs.read(tempBytes, 0, tempBytes.length)) != -1) {
            baos.write(tempBytes, 0, numBytesRead);
        }
        return baos.toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2 ||
                !args[0].equals("--config")) {
            System.out.println("Usage: Archiver --config <file>");
            System.exit(1);
        }

        String confFile = args[1];

        Conf.getInstance().load(confFile);

        Archiver archiver = new Archiver();

        archiver.compressNewData();

        archiver.close();
    }
}

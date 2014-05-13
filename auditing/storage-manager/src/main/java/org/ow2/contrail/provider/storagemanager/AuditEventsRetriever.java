package org.ow2.contrail.provider.storagemanager;

import com.mongodb.*;
import com.mongodb.util.JSON;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuditEventsRetriever {
    private static Logger log = Logger.getLogger(AuditEventsRetriever.class);
    private DBCollection auditLogCollection;

    public AuditEventsRetriever(DB db) throws IOException {
        this.auditLogCollection = db.getCollection(Conf.AUDIT_LOG_COLL_NAME);
    }

    public String find(String searchCriteriaJson, Date startTime, Date endTime,
                       Integer skip, Integer limit) throws Exception {

        BasicDBObject searchQuery = (BasicDBObject) JSON.parse(searchCriteriaJson);

        if (startTime != null || endTime != null) {
            BasicDBObjectBuilder eventTime = new BasicDBObjectBuilder();
            if (startTime != null) {
                eventTime.append("$gte", startTime);
            }
            if (endTime != null) {
                eventTime.append("$lt", endTime);
            }
            searchQuery.put("eventTime", eventTime.get());
        }

        BasicDBObject sortBy = new BasicDBObject("eventTime", 1);

        DBCursor cursor = null;

        try {
            DBObject exclude = new BasicDBObject("_id", 0);
            cursor = auditLogCollection.find(searchQuery, exclude).sort(sortBy);
            if (skip != null && limit != null) {
                cursor = cursor.skip(skip).limit(limit);
            }
            List<DBObject> auditRecords = new ArrayList<DBObject>();
            while (cursor.hasNext()) {
                DBObject auditRecord = cursor.next();
                auditRecords.add(auditRecord);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(auditRecords);
        }
        catch (Exception e) {
            throw new Exception("Failed to retrieve audit records: " + e.getMessage(), e);
        }
        finally {
            if (cursor != null)
                cursor.close();
        }
    }
}

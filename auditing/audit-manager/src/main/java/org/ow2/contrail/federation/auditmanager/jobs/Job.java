package org.ow2.contrail.federation.auditmanager.jobs;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.ow2.contrail.federation.auditmanager.utils.Conf;
import org.ow2.contrail.federation.auditmanager.utils.MongoDBConnection;

import java.util.Date;
import java.util.UUID;

public abstract class Job implements Runnable {
    protected JobStatus jobStatus;
    protected String jobId;
    protected Date created;
    protected DBCollection jobsColl;
    protected ObjectId _id;
    protected Date jobStartTime;
    protected String accessToken;
    protected Exception exception;

    protected Job() {
        jobId = UUID.randomUUID().toString();
        created = new Date();

        DB mongoDB = MongoDBConnection.getDB();
        jobsColl = mongoDB.getCollection(Conf.MONGO_JOBS_COLLECTION);
    }

    public String getJobId() {
        return jobId;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Exception getException() {
        return exception;
    }

    public DBObject export() throws Exception {
        DBObject jobData = new BasicDBObject();
        jobData.put("jobId", jobId);
        jobData.put("created", created);
        jobData.put("status", jobStatus.name());
        jobData.put("type", this.getClass().getSimpleName());
        return jobData;
    }

    public void persist() throws Exception {
        DBObject jobData = this.export();
        jobsColl.insert(jobData);
        _id = (ObjectId) jobData.get("_id");
    }

    public Double getJobExecutionTime() {
        if (jobStartTime == null) {
            return null;
        }

        Date endTime = new Date();
        return (endTime.getTime() - jobStartTime.getTime()) / 1000.0;
    }
}

package org.ow2.contrail.federation.auditmanager.jobs;

import com.mongodb.*;
import com.mongodb.util.JSON;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ow2.contrail.common.oauth.client.AccessToken;
import org.ow2.contrail.common.oauth.client.OAuthHttpClient;
import org.ow2.contrail.common.oauth.client.atmanager.OAuthATManager;
import org.ow2.contrail.common.oauth.client.atmanager.OAuthATManagerFactory;
import org.ow2.contrail.federation.auditmanager.utils.Conf;
import org.ow2.contrail.federation.auditmanager.utils.DateUtils;
import org.ow2.contrail.federation.auditmanager.utils.MongoDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class AuditEventsGathererJob extends Job {
    private static Logger log = LoggerFactory.getLogger(AuditEventsGathererJob.class);
    private static final String TIMELINE_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private JSONObject filter;
    private Date startTime;
    private Date endTime;

    private String keystoreFile;
    private String keystorePass;
    private String truststoreFile;
    private String truststorePass;

    public AuditEventsGathererJob(JSONObject filter, Date startTime, Date endTime) {
        this.filter = filter;
        this.startTime = startTime;
        this.endTime = endTime;

        keystoreFile = Conf.getInstance().getKeystoreFile();
        keystorePass = Conf.getInstance().getKeystorePass();
        truststoreFile = Conf.getInstance().getTruststoreFile();
        truststorePass = Conf.getInstance().getTruststorePass();
    }

    @Override
    public DBObject export() throws Exception {
        DBObject jobData = super.export();
        jobData.put("filter", filter.toString());
        jobData.put("startTime", startTime);
        jobData.put("endTime", endTime);
        return jobData;
    }

    @Override
    public void run() {
        log.debug("Job {} started.", jobId);
        jobStatus = JobStatus.RUNNING;
        jobStartTime = new Date();

        try {
            log.debug("Obtaining an access token to query federation-api...");
            OAuthATManager oauthATManager = OAuthATManagerFactory.getOAuthATManager();
            AccessToken accessTokenForFedApi = oauthATManager.getAccessToken("FEDERATION");

            OAuthHttpClient oAuthHttpClient = new OAuthHttpClient(
                    keystoreFile, keystorePass,
                    truststoreFile, truststorePass);

            log.debug("Querying federation-api for list of all registered auditing services...");

            URI fapiTargetUri = UriBuilder.fromUri(Conf.getInstance().getAddressFederationApi())
                    .path("services")
                    .queryParam("name", "auditing").build();
            HttpResponse serviceResponse = oAuthHttpClient.get(fapiTargetUri, accessTokenForFedApi.getValue());
            String respContent = oAuthHttpClient.getContent(serviceResponse);

            if (serviceResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.error(String.format("Failed to query federation-api: %s\n%s",
                        serviceResponse.getStatusLine(), respContent));
                throw new Exception(String.format(
                        "Failed to obtain list of registered auditing services from the federation-api: %s",
                        serviceResponse.getStatusLine()));
            }
            JSONArray auditingServices = new JSONArray(respContent);
            log.debug("List of registered auditing services obtained successfully: " + auditingServices.toString());

            JSONObject searchCriteria = new JSONObject();

            if (filter.has("user")) {
                String userUuid = filter.getString("user");
                log.debug(String.format(
                        "User filter specified. Retrieving all access tokens for the user %s from the oauth-as.",
                        userUuid));
                String url = Conf.getInstance().getAddressOAuthAS() +
                        String.format("admin/owners/%s/access_tokens", userUuid);
                HttpResponse asResponse = oAuthHttpClient.get(new URI(url), accessTokenForFedApi.getValue());
                if (asResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    log.error(String.format("Failed to query oauth-as: %s\n%s",
                            serviceResponse.getStatusLine(), respContent));
                    throw new Exception(String.format(
                            "Failed to obtain list of user's access tokens from the oauth-as: %s",
                            serviceResponse.getStatusLine()));
                }

                String tokensDataString = oAuthHttpClient.getContent(asResponse);
                JSONArray tokensData = new JSONArray(tokensDataString);

                JSONArray tokens = new JSONArray();
                for (int i = 0; i < tokensData.length(); i++) {
                    JSONObject tokenData = tokensData.getJSONObject(i);
                    tokens.put(tokenData.getString("access_token"));
                }
                log.debug("Successfully retrieved list of user's access tokens.");

                JSONObject inExpr = new JSONObject();
                inExpr.put("$in", tokens);
                searchCriteria.put("initiator.oauthAccessToken", inExpr);
            }

            log.debug("Search criteria: " + searchCriteria.toString());

            log.debug("Starting AuditEventsRetriever threads...");
            CountDownLatch latch = new CountDownLatch(auditingServices.length());
            List<AuditEventsRetriever> retrievers = new ArrayList<AuditEventsRetriever>();
            for (int i = 0; i < auditingServices.length(); i++) {
                JSONObject auditingService = auditingServices.getJSONObject(i);
                String address = auditingService.getString("address");
                if (!address.endsWith("/")) {
                    address += "/";
                }

                AuditEventsRetriever retriever = new AuditEventsRetriever(address, latch, searchCriteria);
                retrievers.add(retriever);
                Thread t = new Thread(retriever);
                t.start();
            }

            log.debug("Waiting for all threads to finish...");
            latch.await();
            log.debug("All AuditEventsRetriever threads are finished.");

            // check all detailed statuses and determine job's total status
            jobStatus = JobStatus.SUCCESS;
            for (AuditEventsRetriever retriever : retrievers) {
                if (retriever.status == JobStatus.FAILED) {
                    jobStatus = JobStatus.FAILED;
                }
                else if (retriever.status == JobStatus.ERROR) {
                    jobStatus = JobStatus.ERROR;
                    break;
                }
            }

            // store status
            BasicDBObject update = new BasicDBObject();
            update.append("status", jobStatus.name())
                    .append("executionTime", getJobExecutionTime());
            updateJob(update);

            log.debug("Job {} finished with status {}.", jobId, jobStatus);
        }
        catch (Exception e) {
            jobStatus = JobStatus.ERROR;
            exception = e;
            log.error(String.format("Job %s failed: %s", jobId, e.getMessage()), e);
        }
    }

    private void updateJob(BasicDBObject o) {
        BasicDBObject searchQuery = new BasicDBObject().append("_id", _id);
        BasicDBObject update = new BasicDBObject();
        update.append("$set", o);
        jobsColl.update(searchQuery, update);
    }

    class AuditEventsRetriever implements Runnable {

        private String auditingService;
        private CountDownLatch latch;
        private JSONObject searchCriteria;
        private JobStatus status;

        public AuditEventsRetriever(String auditingService, CountDownLatch latch, JSONObject
                searchCriteria) {
            this.auditingService = auditingService;
            this.latch = latch;
            this.searchCriteria = searchCriteria;
            status = JobStatus.RUNNING;
        }

        @Override
        public void run() {
            try {
                log.debug("AuditEventsRetriever for the auditing service '{}' started.", auditingService);

                OAuthHttpClient oAuthHttpClient = new OAuthHttpClient(
                        keystoreFile, keystorePass,
                        truststoreFile, truststorePass);

                URI target = new URI(auditingService).resolve("audit_events");

                JSONObject requestParams = new JSONObject();
                requestParams.put("searchCriteria", searchCriteria);
                if (startTime != null) {
                    requestParams.put("startTime", DateUtils.format(startTime));
                }
                if (endTime != null) {
                    requestParams.put("endTime", DateUtils.format(endTime));
                }
                HttpEntity entity = new StringEntity(requestParams.toString(), ContentType.APPLICATION_JSON);

                log.debug("Sending request to " + target);
                HttpResponse response = oAuthHttpClient.post(target, accessToken, entity);
                log.debug("Received response: " + response.getStatusLine());

                String content = getContent(response);

                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    log.debug("Invalid response received:\n{}\n{}", response.getStatusLine().toString(), content);
                    throw new Exception("Invalid response received from auditing-api: " +
                            response.getStatusLine().toString());
                }

                // store data to MongoDB
                DBObject auditEvents = (DBObject) JSON.parse(content);
                BasicDBObject searchQuery = new BasicDBObject().append("_id", _id);
                BasicDBObjectBuilder updateCommand = BasicDBObjectBuilder.start()
                        .append("$push", new BasicDBObject("result.data",
                                new BasicDBObject("$each", auditEvents)))
                        .append("$set", new BasicDBObject(
                                String.format("result.status.%s", auditingService),
                                JobStatus.SUCCESS.name()));
                jobsColl.update(searchQuery, updateCommand.get());

                status = JobStatus.SUCCESS;
                log.debug("AuditEventsRetriever for the auditing service {} finished successfully.", auditingService);
            }
            catch (Exception e) {
                log.error(String.format(
                        "Failed to obtain audit events from the auditing service %s: %s",
                        auditingService, e.getMessage()), e);
                status = JobStatus.ERROR;
            }
            finally {
                latch.countDown();
            }
        }

        public String getContent(HttpResponse response) throws IOException {
            Scanner scanner = new Scanner(response.getEntity().getContent(), "UTF-8").useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    public static DBObject getReportInfo(String jobId) {
        DB mongoDB = MongoDBConnection.getDB();
        DBCollection jobsColl = mongoDB.getCollection(Conf.MONGO_JOBS_COLLECTION);

        BasicDBObject searchQuery = new BasicDBObject().append("jobId", jobId);
        BasicDBObject fields = new BasicDBObject()
                .append("status", 1)
                .append("executionTime", 1);
        return jobsColl.findOne(searchQuery, fields);
    }

    public static BasicDBList getEvents(String jobId, Integer offset, Integer limit) throws JSONException {
        DB mongoDB = MongoDBConnection.getDB();
        DBCollection jobsColl = mongoDB.getCollection(Conf.MONGO_JOBS_COLLECTION);

        BasicDBObject searchQuery = new BasicDBObject().append("jobId", jobId);
        BasicDBObject fields = new BasicDBObject();
        if (offset != null && limit != null) {
            BasicDBList sliceArgs = new BasicDBList();
            sliceArgs.add(offset);
            sliceArgs.add(limit);
            fields.append("result.data", new BasicDBObject().append("$slice", sliceArgs));
        }
        else {
            fields.append("result.data", 1);
        }

        DBObject jobData = jobsColl.findOne(searchQuery, fields);
        return (BasicDBList) ((DBObject) jobData.get("result")).get("data");
    }

    public static BasicDBObject getEvent(String jobId, String eventId) throws JSONException {
        DB mongoDB = MongoDBConnection.getDB();
        DBCollection jobsColl = mongoDB.getCollection(Conf.MONGO_JOBS_COLLECTION);

        BasicDBObject searchQuery = new BasicDBObject().append("jobId", jobId);

        // TODO: retrieve only requested event from MongoDB if possible
        /*BasicDBObject fields = new BasicDBObject();
        BasicDBObject elemMatch = new BasicDBObject("id", eventId);
        fields.append("result.data", new BasicDBObject("$elemMatch", elemMatch));*/

        DBObject jobData = jobsColl.findOne(searchQuery);
        if (jobData == null) {
            return null;
        }

        BasicDBList events = (BasicDBList) ((DBObject) jobData.get("result")).get("data");
        for (int i = 0; i < events.size(); i++) {
            BasicDBObject event = (BasicDBObject) events.get(i);
            if (event.get("id").equals(eventId)) {
                return event;
            }
        }
        return null;
    }

    public static JSONObject getTimeline(String jobId) throws JSONException {
        DB mongoDB = MongoDBConnection.getDB();
        DBCollection jobsColl = mongoDB.getCollection(Conf.MONGO_JOBS_COLLECTION);

        BasicDBObject searchQuery = new BasicDBObject().append("jobId", jobId);
        BasicDBObject fields = new BasicDBObject();
        fields.append("result.data", 1);
        DBObject jobData = jobsColl.findOne(searchQuery, fields);
        if (jobData == null) {
            return null;
        }

        JSONObject timelineData = new JSONObject();

        JSONArray timelineEvents = new JSONArray();
        timelineData.put("events", timelineEvents);

        BasicDBList auditEvents = (BasicDBList) ((DBObject) jobData.get("result")).get("data");

        // startTime and endTime
        if (auditEvents.size() > 0) {
            BasicDBObject first = (BasicDBObject) auditEvents.get(0);
            BasicDBObject last = (BasicDBObject) auditEvents.get(auditEvents.size() - 1);
            timelineData.put("startTime", first.getLong("eventTime"));
            timelineData.put("endTime", last.getLong("eventTime"));
        }

        for (int i = 0; i < auditEvents.size(); i++) {
            BasicDBObject auditEvent = (BasicDBObject) auditEvents.get(i);
            BasicDBObject initiator = (BasicDBObject) auditEvent.get("initiator");
            BasicDBObject target = (BasicDBObject) auditEvent.get("target");

            JSONObject timelineEvent = new JSONObject();
            timelineEvent.put("id", auditEvent.getString("id"));
            timelineEvent.put("time", auditEvent.getLong("eventTime"));
            timelineEvent.put("target", target.getString("id"));
            timelineEvent.put("token", initiator.getString("oauthAccessToken"));
            timelineEvents.put(timelineEvent);
        }
        return timelineData;
    }

    public static JSONArray getAccessTokens(String jobId) throws JSONException {
        DB mongoDB = MongoDBConnection.getDB();
        DBCollection jobsColl = mongoDB.getCollection(Conf.MONGO_JOBS_COLLECTION);

        BasicDBObject searchQuery = new BasicDBObject().append("jobId", jobId);
        BasicDBObject fields = new BasicDBObject();
        fields.append("result.data", 1);
        DBObject jobData = jobsColl.findOne(searchQuery, fields);
        if (jobData == null) {
            return null;
        }

        BasicDBList auditEvents = (BasicDBList) ((DBObject) jobData.get("result")).get("data");
        LinkedHashSet<String> tokensSet = new LinkedHashSet<String>();

        for (int i = 0; i < auditEvents.size(); i++) {
            BasicDBObject auditEvent = (BasicDBObject) auditEvents.get(i);
            BasicDBObject initiator = (BasicDBObject) auditEvent.get("initiator");

            if (initiator.containsField("oauthAccessToken")) {
                tokensSet.add(initiator.getString("oauthAccessToken"));
            }
        }

        JSONArray tokensArray = new JSONArray();
        for (String token : tokensSet) {
            tokensArray.put(token);
        }
        return tokensArray;
    }

    public static JSONArray getInteraction(String jobId) throws JSONException {
        DB mongoDB = MongoDBConnection.getDB();
        DBCollection jobsColl = mongoDB.getCollection(Conf.MONGO_JOBS_COLLECTION);

        BasicDBObject searchQuery = new BasicDBObject().append("jobId", jobId);
        BasicDBObject fields = new BasicDBObject();
        fields.append("result.data", 1);
        DBObject jobData = jobsColl.findOne(searchQuery, fields);
        if (jobData == null) {
            return null;
        }

        JSONArray links = new JSONArray();

        BasicDBList auditEvents = (BasicDBList) ((DBObject) jobData.get("result")).get("data");

        for (int i = 0; i < auditEvents.size(); i++) {
            BasicDBObject auditEvent = (BasicDBObject) auditEvents.get(i);
            BasicDBObject initiator = (BasicDBObject) auditEvent.get("initiator");
            BasicDBObject target = (BasicDBObject) auditEvent.get("target");

            JSONObject link = new JSONObject();
            link.put("id", auditEvent.getString("id"));
            link.put("source", initiator.getString("id"));
            link.put("target", target.getString("id"));
            link.put("token", initiator.getString("oauthAccessToken"));
            links.put(link);
        }
        return links;
    }
}

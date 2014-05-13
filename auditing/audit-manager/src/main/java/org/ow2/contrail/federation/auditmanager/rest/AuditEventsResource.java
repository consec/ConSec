package org.ow2.contrail.federation.auditmanager.rest;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ow2.contrail.common.oauth.client.OAuthFilter;
import org.ow2.contrail.common.oauth.client.TokenInfo;
import org.ow2.contrail.federation.auditmanager.jobs.AuditEventsGathererJob;
import org.ow2.contrail.federation.auditmanager.jobs.JobStatus;
import org.ow2.contrail.federation.auditmanager.scheduler.Scheduler;
import org.ow2.contrail.federation.auditmanager.scheduler.SchedulerFactory;
import org.ow2.contrail.federation.auditmanager.utils.Conf;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Date;

@Path("/audit_events")
public class AuditEventsResource {
    private static Logger log = Logger.getLogger(AuditEventsResource.class);

    @Context
    UriInfo uriInfo;

    @Context
    HttpServletRequest httpServletRequest;

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response startAuditEventsGathererJob(JSONObject data) {

        JSONObject filter;
        Date startTime;
        Date endTime;
        TokenInfo tokenInfo;
        Mode mode;
        int timeout;

        try {
            if (data.has("filter")) {
                filter = data.getJSONObject("filter");
            }
             else {
                filter = new JSONObject();
            }
            startTime = new Date(data.getLong("startTime"));
            endTime = new Date(data.getLong("endTime"));
            mode = data.has("mode") ? Mode.valueOf(data.getString("mode")) : Mode.ASYNC;
            timeout = data.has("timeout") ? data.getInt("timeout") * 1000 : Conf.JOB_TIMEOUT * 1000;

            tokenInfo = OAuthFilter.getAccessTokenInfo(httpServletRequest);
            if (tokenInfo == null) {
                throw new Exception("Missing OAuth access token.");
            }
        }
        catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).
                            entity("Invalid JSON data: " + e.getMessage()).build());
        }

        try {
            log.debug("Starting AuditEventsGathererJob...");
            AuditEventsGathererJob job = new AuditEventsGathererJob(filter, startTime, endTime);
            job.setAccessToken(tokenInfo.getAccessToken());

            Scheduler scheduler = SchedulerFactory.getScheduler();
            scheduler.addJob(job);
            log.debug("AuditEventsGathererJob started successfully.");
            log.debug("Job is running in " + mode + " mode.");

            if (mode.equals(Mode.SYNC)) {
                log.debug("Waiting for the job to finish...");
                long jobStartTime = new Date().getTime();
                while ((job.getJobStatus() == JobStatus.QUEUED || job.getJobStatus() == JobStatus.RUNNING) &&
                        (new Date().getTime() - jobStartTime < timeout)) {
                    Thread.sleep(100);
                }

                if (job.getJobStatus() != JobStatus.QUEUED && job.getJobStatus() != JobStatus.RUNNING) {
                    log.debug("Job has finished.");
                }
                else {
                    log.debug("Timeout.");
                }

                JSONObject statusJson = new JSONObject();
                statusJson.put("jobStatus", job.getJobStatus());
                statusJson.put("executionTime", job.getJobExecutionTime());

                if (job.getJobStatus().equals(JobStatus.SUCCESS)) {
                    URI contentUri = UriBuilder.fromResource(AuditEventsResource.class)
                            .path("reports/{jobId}/content").build(job.getJobId());
                    statusJson.put("reportUri", contentUri);
                }
                else if (job.getJobStatus().equals(JobStatus.ERROR)) {
                    statusJson.put("error", job.getException().toString());
                }

                URI location = uriInfo.getAbsolutePathBuilder().path("reports/{jobId}").build(job.getJobId());
                return Response.created(location).entity(statusJson).build();
            }
            else {
                URI location = uriInfo.getAbsolutePathBuilder().path("reports/{jobId}").build(job.getJobId());
                return Response.created(location).build();
            }
        }
        catch (Exception e) {
            log.error("Failed to retrieve audit events: " + e.getMessage(), e);
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                            entity("Failed to retrieve audit events: " + e.getMessage()).build());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("reports/{jobId}")
    public Response getStatus(@PathParam("jobId") String jobId) throws JSONException {
        JSONObject statusJson = new JSONObject();

        Scheduler scheduler = SchedulerFactory.getScheduler();
        AuditEventsGathererJob job = scheduler.getJob(jobId, AuditEventsGathererJob.class);
        if (job != null) {
            // job is still running
            statusJson.put("jobStatus", job.getJobStatus());
            statusJson.put("executionTime", job.getJobExecutionTime());
        }
        else {
            // there is no active job with that id. Check if corresponding report exists.
            DBObject report = AuditEventsGathererJob.getReportInfo(jobId);
            if (report != null) {
                statusJson.put("jobStatus", report.get("status"));
                statusJson.put("executionTime", report.get("executionTime"));

                if (report.get("status").equals(JobStatus.SUCCESS.name())) {
                    URI contentUri = UriBuilder.fromResource(AuditEventsResource.class)
                            .path("reports/{jobId}/content").build(jobId);
                    statusJson.put("reportUri", contentUri);
                }
                else {
                    statusJson.put("errorMsg", report.get("errorMsg"));
                }
            }
            else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }

        return Response.ok(statusJson.toString()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("reports/{jobId}/content")
    public Response getReport(@PathParam("jobId") String jobId,
                              @QueryParam("offset") Integer offset,
                              @QueryParam("limit") Integer limit) throws JSONException {

        BasicDBList events = AuditEventsGathererJob.getEvents(jobId, offset, limit);
        if (events != null) {
            return Response.ok(events.toString()).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("reports/{jobId}/events/{eventId}")
    public Response getEvent(@PathParam("jobId") String jobId,
                             @QueryParam("offset") Integer offset,
                             @QueryParam("limit") Integer limit,
                             @PathParam("eventId") String eventId) throws JSONException {

        DBObject event = AuditEventsGathererJob.getEvent(jobId, eventId);
        if (event != null) {
            return Response.ok(event.toString()).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("reports/{jobId}/timeline")
    public Response getTimeline(@PathParam("jobId") String jobId) throws JSONException {

        JSONObject timeline = AuditEventsGathererJob.getTimeline(jobId);
        if (timeline != null) {
            return Response.ok(timeline.toString()).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("reports/{jobId}/access_tokens")
    public Response getAccessTokens(@PathParam("jobId") String jobId) throws JSONException {

        JSONArray accessTokensArray = AuditEventsGathererJob.getAccessTokens(jobId);
        if (accessTokensArray != null) {
            return Response.ok(accessTokensArray.toString()).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("reports/{jobId}/interaction")
    public Response getInteraction(@PathParam("jobId") String jobId) throws JSONException {
        JSONArray interaction = AuditEventsGathererJob.getInteraction(jobId);
        if (interaction != null) {
            return Response.ok(interaction.toString()).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private enum Mode {
        SYNC,
        ASYNC
    }
}

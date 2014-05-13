package org.ow2.contrail.resource.auditingapi.rest;

import com.mongodb.DB;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.ow2.contrail.provider.storagemanager.AuditEventsRetriever;
import org.ow2.contrail.resource.auditingapi.utils.DateUtils;
import org.ow2.contrail.resource.auditingapi.utils.MongoDBConnection;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Date;

@Path("/audit_events")
public class AuditEventsResource {
    private static Logger log = Logger.getLogger(AuditEventsResource.class);

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public String getAuditEvents(JSONObject data) {

        JSONObject searchCriteria;
        Date startTime = null;
        Date endTime = null;
        Integer offset = null;
        Integer limit = null;

        try {
            if (data.has("searchCriteria")) {
                searchCriteria = data.getJSONObject("searchCriteria");
            }
            else {
                searchCriteria = new JSONObject();
            }

            if (data.has("startTime")) {
                startTime = DateUtils.parseDate(data.getString("startTime"));
            }

            if (data.has("endTime")) {
                endTime = DateUtils.parseDate(data.getString("endTime"));
            }

            if (data.has("offset")) {
                offset = data.getInt("offset");
            }

            if (data.has("limit")) {
                limit = data.getInt("limit");
            }
        }
        catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).
                            entity("Invalid JSON data: " + e.getMessage()).build());
        }

        try {
            log.debug("Retrieving audit events from storage-manager...");
            DB db = MongoDBConnection.getDB();
            AuditEventsRetriever auditEventsRetriever = new AuditEventsRetriever(db);
            String json = auditEventsRetriever.find(searchCriteria.toString(), startTime, endTime, offset, limit);
            log.debug("Audit events retrieved successfully.");

            return json;
        }
        catch (Exception e) {
            log.error("Failed to retrieve audit events: " + e.getMessage(), e);
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                            entity("Failed to retrieve audit events: " + e.getMessage()).build());
        }
    }
}

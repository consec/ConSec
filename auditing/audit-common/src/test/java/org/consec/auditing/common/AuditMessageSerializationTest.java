package org.consec.auditing.common;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.consec.auditing.common.cadf.*;
import org.consec.auditing.common.cadf.ext.Initiator;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class AuditMessageSerializationTest {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @Test
    public void testCADFEventRecord() throws Exception {
        CADFEventRecord cadfEvent = createCADFEventRecord();

        ObjectMapper objectMapper = createObjectMapper();
        String json = objectMapper.writeValueAsString(cadfEvent);
        System.out.println(json);

        CADFEventRecord cadfEvent1 = objectMapper.readValue(json, CADFEventRecord.class);

        assertEquals(cadfEvent.getId(), cadfEvent1.getId());

        // initiator
        assertEquals(cadfEvent.getInitiator().getId(), cadfEvent1.getInitiator().getId());
        assertEquals(cadfEvent1.getInitiator().getClass(), Initiator.class);
        assertEquals(cadfEvent1.getInitiator().getTypeURI(), Initiator.TYPE_URI);
        assertEquals(((Initiator) cadfEvent.getInitiator()).getOauthAccessToken(),
                ((Initiator) cadfEvent1.getInitiator()).getOauthAccessToken());

        // target
        assertEquals(cadfEvent.getTarget().getId(), cadfEvent1.getTarget().getId());
        assertEquals(cadfEvent1.getTarget().getClass(), Resource.class);
        assertEquals(cadfEvent1.getTarget().getTypeURI(), Resource.TYPE_URI);
    }

    private CADFEventRecord createCADFEventRecord() {
        CADFEventRecord event = new CADFEventRecord();
        event.setId(UUID.randomUUID().toString());
        event.setEventType(EventType.ACTIVITY);
        event.setEventTime(new Date());
        event.setAction("create");
        event.setOutcome(Outcome.SUCCESS);

        Initiator initiator = new Initiator();
        initiator.setId("contrail-federation-cli");
        initiator.setOauthAccessToken("f74a6db9-9afb-4788-ae66-93bb768b777e");
        event.setInitiator(initiator);

        Resource target = new Resource();
        target.setId("contrail-federation-api");
        List<Attachment> attachments = new ArrayList<Attachment>();
        Attachment requestData = new Attachment();
        attachments.add(requestData);
        requestData.setContentType("application-json");
        requestData.setContent("{\"method\":\"POST\",\"uri\":\"http://contrail.xlab.si:8080/federation-api/providers/b9d3e839-347e-4382-ba30-5cda312ad55f/servers\",\"content\":{\"name\":\"server001.myprovider.com\"}}");
        target.setAttachments(attachments);
        Geolocation geolocation = new Geolocation();
        geolocation.setRegionICANN("si");
        geolocation.setCity("Ljubljana");
        target.setGeolocation(geolocation);
        event.setTarget(target);

        return event;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        objectMapper.setDateFormat(sdf);
        return objectMapper;
    }
}

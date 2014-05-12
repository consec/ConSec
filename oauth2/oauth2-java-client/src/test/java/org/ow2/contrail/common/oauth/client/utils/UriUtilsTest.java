package org.ow2.contrail.common.oauth.client.utils;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class UriUtilsTest {

    @Test
    public void testAppend() throws Exception {
        URI baseUri = new URI("https://localhost:8080/audit-manager/");
        URI targetUri = new URI("https://localhost:8080/audit-manager/audit_events/uuid");
        assertEquals(UriUtils.append(baseUri, "/audit_events/uuid"), targetUri);

        baseUri = new URI("https://localhost:8080/audit-manager");
        assertEquals(UriUtils.append(baseUri, "audit_events/uuid"), targetUri);
    }
}

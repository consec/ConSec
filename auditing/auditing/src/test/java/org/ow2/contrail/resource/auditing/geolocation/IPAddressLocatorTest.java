package org.ow2.contrail.resource.auditing.geolocation;

import org.junit.Test;
import org.ow2.contrail.resource.auditing.cadf.ext.Geolocation;

import java.net.InetAddress;

public class IPAddressLocatorTest {

    @Test
    public void testLocator() throws Exception {
        InetAddress inetAddress = InetAddress.getByName("91.217.255.5");
        IPAddressLocator locator = IPAddressLocatorFactory.getInstance();
        Geolocation geolocation = locator.getGeolocation(inetAddress);
        System.out.println(geolocation);
    }
}

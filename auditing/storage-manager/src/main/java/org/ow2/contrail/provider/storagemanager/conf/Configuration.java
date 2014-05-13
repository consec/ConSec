package org.ow2.contrail.provider.storagemanager.conf;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement(name = "configuration")
public class Configuration {
    private ArrayList<Metric> metrics;

    @XmlElementWrapper(name = "metricsToCompress")
    @XmlElement(name = "metric")
    public ArrayList<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(ArrayList<Metric> metrics) {
        this.metrics = metrics;
    }
}

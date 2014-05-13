package org.ow2.contrail.provider.storagemanager.conf;

import javax.xml.bind.annotation.XmlAttribute;
import java.util.ArrayList;

public class SummaryData {
    private boolean enabled;
    private ArrayList<Statistic> statistics;

    @XmlAttribute(name = "enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ArrayList<Statistic> getStatistics() {
        return statistics;
    }

    public void setStatistics(ArrayList<Statistic> statistics) {
        this.statistics = statistics;
    }

    public enum Statistic {
        MIN,
        MAX,
        AVERAGE,
        STD_DEV
    }
}

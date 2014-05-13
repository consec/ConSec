package org.ow2.contrail.provider.storagemanager.conf;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"type", "interval", "gzipped"})
public class ValuesData {
    private boolean enabled;
    private Type type;
    private Interval interval;
    private boolean gzipped;

    public ValuesData() {
    }

    public ValuesData(boolean enabled, boolean gzipped, Type type) {
        this.enabled = enabled;
        this.gzipped = gzipped;
        this.type = type;
    }

    @XmlAttribute(name = "enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isGzipped() {
        return gzipped;
    }

    public void setGzipped(boolean gzipped) {
        this.gzipped = gzipped;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Interval getInterval() {
        return interval;
    }

    public void setInterval(Interval interval) {
        this.interval = interval;
    }

    public enum Type {
        ARRAY_OF_VALUES,
        INTERVALS_OF_CONST_VALUE
    }
}

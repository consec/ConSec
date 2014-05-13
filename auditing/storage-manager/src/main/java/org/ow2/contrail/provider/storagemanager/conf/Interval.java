package org.ow2.contrail.provider.storagemanager.conf;

import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"value", "unit"})
public class Interval {
    private int value;
    private String unit;

    public Interval() {
    }

    public Interval(int value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}

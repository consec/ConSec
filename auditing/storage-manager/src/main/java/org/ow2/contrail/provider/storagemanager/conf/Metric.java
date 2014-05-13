package org.ow2.contrail.provider.storagemanager.conf;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "metric")
@XmlType(propOrder = {"name", "targetInterval", "valuesData", "summaryData"})
public class Metric {
    private String name;
    private Interval targetInterval;
    private ValuesData valuesData;
    private SummaryData summaryData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Interval getTargetInterval() {
        return targetInterval;
    }

    public void setTargetInterval(Interval targetInterval) {
        this.targetInterval = targetInterval;
    }

    public ValuesData getValuesData() {
        return valuesData;
    }

    public void setValuesData(ValuesData valuesData) {
        this.valuesData = valuesData;
    }

    public SummaryData getSummaryData() {
        return summaryData;
    }

    public void setSummaryData(SummaryData summaryData) {
        this.summaryData = summaryData;
    }
}
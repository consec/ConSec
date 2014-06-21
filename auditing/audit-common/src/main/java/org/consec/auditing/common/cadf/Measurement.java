package org.consec.auditing.common.cadf;

public class Measurement {
    private String result;
    private Metric metric;
    private String metricId;
    private Resource calculatedBy;

    public Measurement() {
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public String getMetricId() {
        return metricId;
    }

    public void setMetricId(String metricId) {
        this.metricId = metricId;
    }

    public Resource getCalculatedBy() {
        return calculatedBy;
    }

    public void setCalculatedBy(Resource calculatedBy) {
        this.calculatedBy = calculatedBy;
    }
}

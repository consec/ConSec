package org.ow2.contrail.provider.storagemanager.common;

public class Metric {
    private String group;
    private String name;
    private String fullName;

    public Metric(String group, String name) {
        this.group = group;
        this.name = name;
        this.fullName = group + "." + name;
    }

    public Metric(String fullName) throws Exception {
        String[] parts = fullName.split("\\.");
        if (parts.length != 2) {
            throw new Exception(String.format(
                    "Invalid metric full name: '%s'. Should be in the format group.name.", fullName));
        }
        this.group = parts[0];
        this.name = parts[1];
        this.fullName = fullName;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Metric metric = (Metric) o;

        return (group.equals(metric.getGroup()) && name.equals(metric.getName()));
    }

    @Override
    public int hashCode() {
        int result = group != null ? group.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}

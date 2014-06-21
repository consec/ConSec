package org.consec.auditing.common.cadf;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

public enum EventType {
    MONITOR,
    ACTIVITY,
    CONTROL;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static EventType fromJson(String value) {
        return EventType.valueOf(value.toUpperCase());
    }
}

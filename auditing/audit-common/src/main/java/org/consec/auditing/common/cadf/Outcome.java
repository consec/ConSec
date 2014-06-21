package org.consec.auditing.common.cadf;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

public enum Outcome {
    SUCCESS,
    FAILURE,
    UNKNOWN,
    PENDING;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static Outcome fromJson(String value) {
        return Outcome.valueOf(value.toUpperCase());
    }
}

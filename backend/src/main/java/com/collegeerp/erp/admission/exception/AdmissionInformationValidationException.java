package com.collegeerp.erp.admission.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdmissionInformationValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public AdmissionInformationValidationException(Map<String, String> fieldErrors) {
        super("Admission information validation failed");
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}

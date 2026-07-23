package com.jadhavr.erp.admission.enums;

import java.util.EnumSet;
import java.util.Set;

public enum AdmissionDocumentType {
    TENTH_MARKSHEET(true),
    TWELFTH_MARKSHEET(true),
    PROVISIONAL_CERTIFICATE(true),
    TRANSFER_CERTIFICATE(true),
    NATIONALITY_CERTIFICATE(true),
    DOMICILE_CERTIFICATE(true),
    AADHAAR_CARD(true),
    GRADUATION_MARKSHEET(false),
    MIGRATION_CERTIFICATE(false),
    GAP_CERTIFICATE(false),
    ENTRANCE_SCORE_CARD(false),
    CASTE_CERTIFICATE(false),
    CASTE_VALIDITY(false),
    NON_CREAMY_LAYER_CERTIFICATE(false),
    NAME_CHANGE_CERTIFICATE(false),
    INCOME_CERTIFICATE(false),
    FORM_O_MINORITY(false);

    private final boolean required;

    AdmissionDocumentType(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    public static Set<AdmissionDocumentType> requiredTypes() {
        EnumSet<AdmissionDocumentType> required = EnumSet.noneOf(AdmissionDocumentType.class);
        for (AdmissionDocumentType type : values()) if (type.required) required.add(type);
        return required;
    }
}

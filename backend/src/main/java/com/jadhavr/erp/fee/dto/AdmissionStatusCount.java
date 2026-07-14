package com.jadhavr.erp.fee.dto;

import com.jadhavr.erp.admission.enums.AdmissionStatus;

public record AdmissionStatusCount(AdmissionStatus status, Long value) {
}

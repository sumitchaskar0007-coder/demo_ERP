package com.collegeerp.erp.fee.dto;

import com.collegeerp.erp.admission.enums.AdmissionStatus;

public record AdmissionStatusCount(AdmissionStatus status, Long value) {
}

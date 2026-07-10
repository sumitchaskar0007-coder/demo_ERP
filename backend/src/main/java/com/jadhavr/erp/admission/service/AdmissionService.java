package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionResponse;
import com.jadhavr.erp.admission.dto.PublicAdmissionInfoResponse;
import com.jadhavr.erp.admission.dto.SubmitAdmissionRequest;
import com.jadhavr.erp.admission.dto.SubmitAdmissionResponse;

public interface AdmissionService {
    PublicAdmissionInfoResponse getPublicAdmissionInfo(String collegeCode);
    SubmitAdmissionResponse submitAdmission(String collegeCode, SubmitAdmissionRequest request);
    AdmissionResponse getMyLatestAdmission();
}

package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionResponse;
import com.jadhavr.erp.admission.dto.AdmissionPrintResponse;
import com.jadhavr.erp.admission.dto.AdmissionCourseYearOptionResponse;
import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.dto.PublicAdmissionInfoResponse;
import com.jadhavr.erp.admission.dto.StudentAdmissionAccessResponse;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.dto.SubmitAdmissionRequest;
import com.jadhavr.erp.admission.dto.SubmitAdmissionResponse;

public interface AdmissionService {
    PublicAdmissionInfoResponse getPublicAdmissionInfo(String collegeCode);
    SubmitAdmissionResponse submitAdmission(String collegeCode, SubmitAdmissionRequest request);
    AdmissionResponse getMyLatestAdmission();
    StudentSectionAdmissionResponse getMyDetailedAdmission();
    StudentAdmissionAccessResponse getMyAdmissionAccess();
    java.util.List<AdmissionCourseYearOptionResponse> getMyCourseYearOptions();
    StudentSectionAdmissionResponse submitMyAdmissionDetails(DetailedAdmissionRequest request);
    AdmissionPrintResponse getMyAdmissionPrintData();
}

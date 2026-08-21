package com.collegeerp.erp.admission.service;

import com.collegeerp.erp.admission.dto.AdmissionResponse;
import com.collegeerp.erp.admission.dto.AdmissionPrintResponse;
import com.collegeerp.erp.admission.dto.AdmissionCourseYearOptionResponse;
import com.collegeerp.erp.admission.dto.DetailedAdmissionRequest;
import com.collegeerp.erp.admission.dto.PublicAdmissionInfoResponse;
import com.collegeerp.erp.admission.dto.StudentAdmissionAccessResponse;
import com.collegeerp.erp.admission.dto.StudentSectionAdmissionResponse;
import com.collegeerp.erp.admission.dto.SubmitAdmissionRequest;
import com.collegeerp.erp.admission.dto.SubmitAdmissionResponse;

public interface AdmissionService {
    PublicAdmissionInfoResponse getPublicAdmissionInfo(String collegeCode);
    SubmitAdmissionResponse submitAdmission(String collegeCode, SubmitAdmissionRequest request);
    AdmissionResponse getMyLatestAdmission();
    StudentSectionAdmissionResponse getMyDetailedAdmission();
    StudentAdmissionAccessResponse getMyAdmissionAccess();
    java.util.List<AdmissionCourseYearOptionResponse> getMyCourseYearOptions();
    StudentSectionAdmissionResponse submitMyAdmissionDetails(DetailedAdmissionRequest request);
    com.collegeerp.erp.admission.dto.AdmissionInformationValidationResponse validateMyAdmissionDetails(
            DetailedAdmissionRequest request);
    AdmissionPrintResponse getMyAdmissionPrintData();
    com.collegeerp.erp.admission.dto.AdmissionDetailDraftResponse getMyAdmissionDetailDraft();
    com.collegeerp.erp.admission.dto.AdmissionDetailDraftResponse saveMyAdmissionDetailDraft(
            com.collegeerp.erp.admission.dto.AdmissionDetailDraftRequest request);
}

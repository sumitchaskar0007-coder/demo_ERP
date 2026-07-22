package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionPrintResponse;
import com.jadhavr.erp.admission.dto.AdmissionCourseYearOptionResponse;
import com.jadhavr.erp.admission.dto.AdmissionStatusHistoryResponse;
import com.jadhavr.erp.admission.dto.MarkAdmissionPrintedRequest;
import com.jadhavr.erp.admission.dto.RejectAdmissionRequest;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.dto.VerifyAdmissionRequest;
import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.common.dto.PageResponse;

import java.util.List;

public interface StudentSectionAdmissionService {
    PageResponse<StudentSectionAdmissionResponse> searchAdmissionsForStudentSection(
            String keyword, Long departmentId, AdmissionStatus status,
            int page, int size, String sortBy, String sortDir);
    StudentSectionAdmissionResponse getAdmissionForStudentSection(Long admissionId);
    List<AdmissionCourseYearOptionResponse> getCourseYearOptions(Long admissionId);
    StudentSectionAdmissionResponse startReview(Long admissionId);
    StudentSectionAdmissionResponse updateDetails(Long admissionId, DetailedAdmissionRequest request);
    StudentSectionAdmissionResponse approveAdmission(Long admissionId, VerifyAdmissionRequest request);
    StudentSectionAdmissionResponse rejectAdmission(Long admissionId, RejectAdmissionRequest request);
    List<AdmissionStatusHistoryResponse> getAdmissionHistory(Long admissionId);
    AdmissionPrintResponse getPrintData(Long admissionId);
    StudentSectionAdmissionResponse markAdmissionPrinted(Long admissionId, MarkAdmissionPrintedRequest request);
}

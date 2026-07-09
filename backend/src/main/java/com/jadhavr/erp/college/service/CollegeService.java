package com.jadhavr.erp.college.service;

import com.jadhavr.erp.college.dto.CollegeResponse;
import com.jadhavr.erp.college.dto.CreateCollegeRequest;
import com.jadhavr.erp.college.dto.UpdateCollegeRequest;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.common.dto.PageResponse;

import java.util.List;

public interface CollegeService {

    CollegeResponse createCollege(CreateCollegeRequest request);

    List<CollegeResponse> getAllColleges();

    CollegeResponse getCollegeById(Long id);

    CollegeResponse getCollegeByCode(String code);

    CollegeResponse updateCollege(Long id, UpdateCollegeRequest request);

    CollegeResponse activateCollege(Long id);

    CollegeResponse deactivateCollege(Long id);

    List<CollegeResponse> getActiveColleges();

    PageResponse<CollegeResponse> searchColleges(
            String keyword, CollegeStatus status, int page, int size, String sortBy, String sortDir);
}

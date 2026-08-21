package com.collegeerp.erp.college.service;

import com.collegeerp.erp.college.dto.CollegeResponse;
import com.collegeerp.erp.college.dto.CreateCollegeRequest;
import com.collegeerp.erp.college.dto.UpdateCollegeRequest;
import com.collegeerp.erp.college.entity.CollegeStatus;
import com.collegeerp.erp.common.dto.PageResponse;

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

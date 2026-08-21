package com.collegeerp.erp.student.service;

import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.student.dto.StudentProfileResponse;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.student.mapper.StudentProfileMapper;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProfileServiceImpl implements StudentProfileService {
    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileMapper studentProfileMapper;

    public StudentProfileServiceImpl(
            StudentProfileRepository studentProfileRepository,
            StudentProfileMapper studentProfileMapper) {
        this.studentProfileRepository = studentProfileRepository;
        this.studentProfileMapper = studentProfileMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile() {
        StudentProfile profile = studentProfileRepository.findByUserId(currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        return studentProfileMapper.toResponse(profile);
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails details) {
            return details.getId();
        }
        throw new BadRequestException("Authenticated user is invalid");
    }
}

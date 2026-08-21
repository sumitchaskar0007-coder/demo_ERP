package com.collegeerp.erp.academic.controller;

import com.collegeerp.erp.academic.dto.StudentAcademicAccessResponse;
import com.collegeerp.erp.academic.entity.StudentSectionEnrollment;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.collegeerp.erp.academic.service.AcademicService;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.timetable.service.WeeklyTimetableService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/academic")
public class StudentAcademicController {
    private final StudentProfileRepository students;
    private final StudentSectionEnrollmentRepository enrollments;
    private final AcademicService service;
    private final WeeklyTimetableService weeklyTimetables;

    public StudentAcademicController(
            StudentProfileRepository students,
            StudentSectionEnrollmentRepository enrollments,
            AcademicService service,
            WeeklyTimetableService weeklyTimetables) {
        this.students = students;
        this.enrollments = enrollments;
        this.service = service;
        this.weeklyTimetables = weeklyTimetables;
    }

    @GetMapping("/access-state")
    public ApiResponse<StudentAcademicAccessResponse> accessState() {
        StudentAcademicAccessResponse access = enrollments
                .findFirstByStudentAndStatus(me(), AcademicStatus.ACTIVE)
                .map(this::toAccessResponse)
                .orElseGet(StudentAcademicAccessResponse::notAllocated);
        return ApiResponse.success("Student academic access retrieved", access);
    }

    @GetMapping("/timetable")
    public ApiResponse<?> timetable() {
        StudentSectionEnrollment enrollment = enrollments
                .findFirstByStudentAndStatus(me(), AcademicStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Student is not assigned to a division"));
        return ApiResponse.success("My timetable", weeklyTimetables.studentTimetable(enrollment));
    }

    @GetMapping({"/attendance", "/attendance/summary"})
    public ApiResponse<?> attendance() {
        return ApiResponse.success("My attendance", service.attendanceSummary(me().getId()));
    }

    @GetMapping("/class")
    public ApiResponse<?> studentClass() {
        return ApiResponse.success("My class", service.studentClass());
    }

    private StudentAcademicAccessResponse toAccessResponse(StudentSectionEnrollment enrollment) {
        return new StudentAcademicAccessResponse(
                true,
                enrollment.getAcademicClass().getId(),
                enrollment.getAcademicClass().getName(),
                enrollment.getSection().getId(),
                enrollment.getSection().getName(),
                enrollment.getAcademicYear(),
                enrollment.getRollNumber());
    }

    private StudentProfile me() {
        return students.findByUserId(SecurityUtils.requireCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
    }
}

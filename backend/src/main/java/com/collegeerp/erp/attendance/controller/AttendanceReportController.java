package com.collegeerp.erp.attendance.controller;

import com.collegeerp.erp.attendance.service.WeeklyAttendanceService;
import com.collegeerp.erp.common.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
public class AttendanceReportController {
    private final WeeklyAttendanceService service;
    public AttendanceReportController(WeeklyAttendanceService service){ this.service=service; }

    @GetMapping("/api/class-teacher/attendance/division")
    @PreAuthorize("hasRole('CLASS_TEACHER')")
    public ApiResponse<?> division(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) Long divisionId, @RequestParam(required=false) Long subjectId){
        return ApiResponse.success("Class attendance report", service.classTeacherReport(from,to,divisionId,subjectId));
    }

    @GetMapping("/api/hod/attendance/report")
    @PreAuthorize("hasRole('HOD')")
    public ApiResponse<?> hod(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) Long departmentId, @RequestParam(required=false) Long divisionId,
            @RequestParam(required=false) Long subjectId, @RequestParam(required=false) Long teacherId){
        return ApiResponse.success("Department attendance report", service.hodReport(from,to,departmentId,divisionId,subjectId,teacherId));
    }

    @GetMapping("/api/principal/attendance/report")
    @PreAuthorize("hasAnyRole('PRINCIPAL','SUPER_ADMIN')")
    public ApiResponse<?> principal(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) Long departmentId, @RequestParam(required=false) Long divisionId,
            @RequestParam(required=false) Long subjectId, @RequestParam(required=false) Long teacherId){
        return ApiResponse.success("College attendance report", service.principalReport(from,to,departmentId,divisionId,subjectId,teacherId));
    }

    @GetMapping("/api/principal/attendance/dashboard")
    @PreAuthorize("hasAnyRole('PRINCIPAL','SUPER_ADMIN')")
    public ApiResponse<?> dashboard(){
        LocalDate today=LocalDate.now();
        return ApiResponse.success("Attendance dashboard", service.principalReport(today.minusDays(29),today,null,null,null,null));
    }
}

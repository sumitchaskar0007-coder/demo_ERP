package com.jadhavr.erp.attendance.controller;

import com.jadhavr.erp.attendance.service.WeeklyAttendanceService;
import com.jadhavr.erp.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/attendance")
@PreAuthorize("hasRole('STUDENT')")
public class StudentWeeklyAttendanceController {
    private final WeeklyAttendanceService service;
    public StudentWeeklyAttendanceController(WeeklyAttendanceService service){ this.service=service; }

    @GetMapping public ApiResponse<?> attendance(){ return ApiResponse.success("My attendance", service.studentAttendance(null,null)); }
    @GetMapping("/subject-wise") public ApiResponse<?> subjects(){ return ApiResponse.success("Subject-wise attendance", service.studentSubjectWise()); }
    @GetMapping("/monthly") public ApiResponse<?> monthly(@RequestParam(required=false) Integer year){ return ApiResponse.success("Monthly attendance", service.studentMonthly(year)); }
}

package com.jadhavr.erp.attendance.controller;

import com.jadhavr.erp.attendance.dto.WeeklyAttendanceDtos.*;
import com.jadhavr.erp.attendance.service.WeeklyAttendanceService;
import com.jadhavr.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/teacher/attendance")
@PreAuthorize("hasAnyRole('PRINCIPAL','HOD','SUBJECT_TEACHER','CLASS_TEACHER')")
public class TeacherWeeklyAttendanceController {
    private final WeeklyAttendanceService service;
    public TeacherWeeklyAttendanceController(WeeklyAttendanceService service){ this.service=service; }

    @GetMapping("/current-lecture")
    public ApiResponse<?> current(){ return ApiResponse.success("Current scheduled lecture", service.currentLecture()); }

    @GetMapping("/today-lectures")
    public ApiResponse<?> today(){ return ApiResponse.success("Today's scheduled lectures", service.todayLectures()); }

    @GetMapping("/students")
    public ApiResponse<?> students(@RequestParam Long lectureId){
        return ApiResponse.success("Lecture student roster", service.roster(lectureId));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody MarkRequest request){
        return ApiResponse.success(request.submit() ? "Attendance submitted and locked" : "Attendance draft saved", service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request){
        return ApiResponse.success(request.submit() ? "Attendance submitted and locked" : "Attendance draft updated", service.update(id, request));
    }

    @GetMapping("/history")
    public ApiResponse<?> history(
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) Long subjectId, @RequestParam(required=false) Long divisionId){
        return ApiResponse.success("Teacher attendance history", service.teacherHistory(from,to,subjectId,divisionId));
    }
}

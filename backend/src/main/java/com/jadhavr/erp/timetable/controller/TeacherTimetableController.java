package com.jadhavr.erp.timetable.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.timetable.service.TeacherTimetableService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher/timetable")
@PreAuthorize("hasAnyRole('SUBJECT_TEACHER','CLASS_TEACHER')")
public class TeacherTimetableController {
    private final TeacherTimetableService service;

    public TeacherTimetableController(TeacherTimetableService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<?> timetable() {
        return ApiResponse.success("My weekly timetable", service.timetable());
    }

    @GetMapping("/today")
    public ApiResponse<?> today() {
        return ApiResponse.success("Today's lectures", service.today());
    }

    @GetMapping("/next")
    public ApiResponse<?> next() {
        return ApiResponse.success("Next lecture", service.next());
    }

    @GetMapping("/day/{day}")
    public ApiResponse<?> day(@PathVariable String day) {
        return ApiResponse.success("Teacher day timetable", service.day(day));
    }
}

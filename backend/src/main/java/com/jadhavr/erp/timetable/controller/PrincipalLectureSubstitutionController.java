package com.jadhavr.erp.timetable.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.timetable.dto.LectureSubstitutionDtos.CreateSubstitutionRequest;
import com.jadhavr.erp.timetable.service.LectureSubstitutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/principal")
@PreAuthorize("hasRole('PRINCIPAL')")
public class PrincipalLectureSubstitutionController {
    private final LectureSubstitutionService service;

    public PrincipalLectureSubstitutionController(LectureSubstitutionService service) {
        this.service = service;
    }

    @GetMapping("/staff/{teacherId}/today-lectures")
    public ApiResponse<?> today(@PathVariable Long teacherId) {
        return ApiResponse.success("Today's teacher lectures", service.todayForTeacher(teacherId));
    }

    @GetMapping("/staff/{teacherId}/today-lectures/{entryId}/available-teachers")
    public ApiResponse<?> available(@PathVariable Long teacherId, @PathVariable Long entryId) {
        return ApiResponse.success("Available substitute teachers",
                service.availableTeachers(teacherId, entryId));
    }

    @PostMapping("/lecture-substitutions")
    public ResponseEntity<ApiResponse<?>> create(
            @Valid @RequestBody CreateSubstitutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Lecture forwarded successfully", service.create(request)));
    }

    @DeleteMapping("/lecture-substitutions/{id}")
    public ApiResponse<?> cancel(@PathVariable Long id) {
        service.cancel(id);
        return ApiResponse.success("Lecture forwarding cancelled", null);
    }
}

package com.jadhavr.erp.academic.controller;

import com.jadhavr.erp.academic.dto.AcademicDtos.Assign;
import com.jadhavr.erp.academic.dto.AcademicDtos.AssignStudent;
import com.jadhavr.erp.academic.dto.AcademicDtos.CreateAttendance;
import com.jadhavr.erp.academic.dto.AcademicDtos.CreateClass;
import com.jadhavr.erp.academic.dto.AcademicDtos.CreateSection;
import com.jadhavr.erp.academic.dto.AcademicDtos.CreateSubject;
import com.jadhavr.erp.academic.dto.AcademicDtos.CreateTimetable;
import com.jadhavr.erp.academic.dto.AcademicDtos.Mark;
import com.jadhavr.erp.academic.dto.AcademicDtos.UpdateSubject;
import com.jadhavr.erp.academic.service.AcademicService;
import com.jadhavr.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy academic endpoints. New teacher/student workflows use their dedicated,
 * assignment-scoped controllers. These endpoints deliberately expose setup only
 * to Principal/HOD and attendance mutation only to assigned teachers.
 */
@RestController
@RequestMapping("/api/academic")
public class AcademicController {
    private final AcademicService service;

    public AcademicController(AcademicService service) {
        this.service = service;
    }

    @PostMapping("/classes")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> createClass(@Valid @RequestBody CreateClass request) {
        return ApiResponse.success("Academic class created", service.createClass(request));
    }
    @GetMapping("/classes/search")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> classes(@RequestParam(required = false) Long departmentId) {
        return ApiResponse.success("Academic classes", service.classes(departmentId));
    }

    @PatchMapping("/classes/{id}/activate")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> activateClass(@PathVariable Long id) {
        return ApiResponse.success("Class activated", service.classStatus(id, true));
    }

    @PatchMapping("/classes/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> deactivateClass(@PathVariable Long id) {
        return ApiResponse.success("Class deactivated", service.classStatus(id, false));
    }

    @PostMapping("/sections")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> createSection(@Valid @RequestBody CreateSection request) {
        return ApiResponse.success("Section created", service.createSection(request));
    }

    @GetMapping("/sections/search")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> sections(@RequestParam(required = false) Long classId) {
        return ApiResponse.success("Sections", service.sections(classId));
    }

    @PatchMapping("/sections/{id}/assign-class-teacher")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> assignClassTeacher(@PathVariable Long id, @Valid @RequestBody Assign request) {
        return ApiResponse.success("Class teacher assigned", service.assignClassTeacher(id, request));
    }

    @PostMapping("/sections/{id}/students")
    @PreAuthorize("hasRole('HOD')")
    public ApiResponse<?> assignStudent(@PathVariable Long id, @Valid @RequestBody AssignStudent request) {
        return ApiResponse.success("Student assigned", service.assignStudent(id, request));
    }

    @GetMapping("/classes/{id}/eligible-students")
    @PreAuthorize("hasRole('HOD')")
    public ApiResponse<?> eligibleStudents(@PathVariable Long id) {
        return ApiResponse.success("Eligible students", service.eligibleStudents(id));
    }

    @GetMapping("/sections/{id}/students")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> sectionStudents(@PathVariable Long id) {
        return ApiResponse.success("Class roster", service.sectionStudents(id));
    }

    @GetMapping("/class-teacher/my-class")
    @PreAuthorize("hasRole('CLASS_TEACHER')")
    public ApiResponse<?> classTeacherRoster() {
        return ApiResponse.success("My class roster", service.classTeacherRoster());
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> createSubject(@Valid @RequestBody CreateSubject request) {
        return ApiResponse.success("Subject created", service.createSubject(request));
    }

    @PutMapping("/subjects/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> updateSubject(@PathVariable Long id, @Valid @RequestBody UpdateSubject request) {
        return ApiResponse.success("Subject updated", service.updateSubject(id, request));
    }

    @DeleteMapping("/subjects/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> deleteSubject(@PathVariable Long id) {
        service.deleteSubject(id);
        return ApiResponse.success("Subject deleted", null);
    }

    @GetMapping("/subjects/search")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> subjects(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String yearName) {
        return ApiResponse.success("Subjects", service.subjects(classId, departmentId, yearName));
    }

    @PostMapping("/subjects/{id}/assign-teacher")
    @PreAuthorize("hasRole('HOD')")
    public ApiResponse<?> assignTeacher(@PathVariable Long id, @Valid @RequestBody Assign request) {
        return ApiResponse.success("Subject teacher assigned", service.assignTeacher(id, request));
    }

    @DeleteMapping("/subjects/{subjectId}/unassign-teacher/{teacherId}")
    @PreAuthorize("hasRole('HOD')")
    public ApiResponse<?> unassignTeacher(@PathVariable Long subjectId, @PathVariable Long teacherId) {
        service.unassignTeacher(subjectId, teacherId);
        return ApiResponse.success("Teacher unassigned from subject", null);
    }

    @GetMapping("/subject-teacher-assignments")
    @PreAuthorize("hasRole('HOD')")
    public ApiResponse<?> listAssignments(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long subjectId) {
        return ApiResponse.success("Subject-teacher assignments", service.listAssignments(teacherId, subjectId));
    }

    @PostMapping("/timetable")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> createTimetable(@Valid @RequestBody CreateTimetable request) {
        return ApiResponse.success("Timetable entry created", service.createTimetable(request));
    }

    @GetMapping("/timetable")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> timetable(@RequestParam Long sectionId) {
        return ApiResponse.success("Timetable", service.timetable(sectionId));
    }

    @PostMapping("/attendance/sessions")
    @PreAuthorize("hasAnyRole('CLASS_TEACHER','SUBJECT_TEACHER')")
    public ApiResponse<?> attendance(@Valid @RequestBody CreateAttendance request) {
        return ApiResponse.success("Attendance session created", service.createAttendance(request));
    }

    @PatchMapping("/attendance/sessions/{id}/mark")
    @PreAuthorize("hasAnyRole('CLASS_TEACHER','SUBJECT_TEACHER')")
    public ApiResponse<?> mark(@PathVariable Long id, @Valid @RequestBody Mark request) {
        return ApiResponse.success("Attendance saved", service.mark(id, request));
    }

    @PatchMapping("/attendance/sessions/{id}/submit")
    @PreAuthorize("hasAnyRole('CLASS_TEACHER','SUBJECT_TEACHER')")
    public ApiResponse<?> submit(@PathVariable Long id) {
        return ApiResponse.success("Attendance submitted", service.submit(id));
    }

    @GetMapping("/attendance/students/{id}/summary")
    @PreAuthorize("hasAnyRole('PRINCIPAL','HOD')")
    public ApiResponse<?> summary(@PathVariable Long id) {
        return ApiResponse.success("Attendance summary", service.attendanceSummary(id));
    }
}

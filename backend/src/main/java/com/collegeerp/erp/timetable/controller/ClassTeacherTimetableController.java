package com.collegeerp.erp.timetable.controller;
import com.collegeerp.erp.timetable.service.TimetableService;
import com.collegeerp.erp.timetable.dto.TimetableDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/class-teacher/timetable")
@PreAuthorize("hasRole('CLASS_TEACHER')")
public class ClassTeacherTimetableController {
    private final TimetableService service;
    public ClassTeacherTimetableController(TimetableService s) { service = s; }

    @GetMapping("/my-section")
    public Map<String, Object> mySection() { return service.mySection(); }

    @GetMapping("/subjects")
    public List<Map<String, Object>> subjects() { return service.mySectionSubjects(); }

    @GetMapping("/teachers")
    public List<Map<String, Object>> teachers(@RequestParam Long subjectId) { return service.sectionTeachers(subjectId); }

    @GetMapping("/periods")
    public List<Map<String, Object>> periods() { return service.availablePeriods(); }
}

package com.jadhavr.erp.academic.controller;
import com.jadhavr.erp.academic.dto.AcademicDtos.*; import com.jadhavr.erp.academic.service.AcademicService;
import jakarta.validation.Valid; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/academic")
public class AcademicController {private final AcademicService service;public AcademicController(AcademicService s){service=s;}
 @GetMapping("/masters") public List<MasterResponse> list(@RequestParam String type){return service.list(type);}
 @GetMapping("/people") public List<MasterResponse> people(@RequestParam(defaultValue="TEACHERS") String kind){return service.people(kind);}
 @PostMapping("/masters") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PRINCIPAL','HOD')") public MasterResponse create(@Valid @RequestBody MasterRequest r){return service.create(r);}
 @PostMapping("/assignments") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PRINCIPAL','HOD')") public MasterResponse assign(@Valid @RequestBody AssignmentRequest r){return service.assign(r);}
 @PostMapping("/enrollments") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PRINCIPAL','HOD','STUDENT_SECTION')") public MasterResponse enroll(@Valid @RequestBody EnrollmentRequest r){return service.enroll(r);}
}

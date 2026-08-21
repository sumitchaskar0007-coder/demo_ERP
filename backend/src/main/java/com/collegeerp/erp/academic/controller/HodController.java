package com.collegeerp.erp.academic.controller;

import com.collegeerp.erp.academic.dto.HodModuleDtos.*;
import com.collegeerp.erp.academic.service.HodModuleService;
import com.collegeerp.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hod")
@PreAuthorize("hasAnyRole('HOD','PRINCIPAL')")
public class HodController {
    private final HodModuleService service;
    public HodController(HodModuleService service){this.service=service;}

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard(@RequestParam(required=false)Long departmentId){
        Summary s=service.dashboard(departmentId);Map<String,Object> data=new LinkedHashMap<>();
        data.put("totalStudents",s.totalStudents());data.put("totalTeachers",s.totalTeachers());
        data.put("totalClasses",s.totalDivisions());data.put("totalSections",s.totalDivisions());
        data.put("totalSubjects",s.totalSubjects());data.put("todayAttendanceSessions",s.classesRunningToday());
        data.put("averageAttendancePercentage",s.averageAttendance());data.put("pendingTasks",s.pendingTasks());
        return ApiResponse.success("HOD dashboard",data);
    }

    @GetMapping("/workspace")
    public ApiResponse<?> workspace(@RequestParam(required=false)Long departmentId,@RequestParam(required=false)String search,
            @RequestParam(required=false)Long courseYearId,@RequestParam(required=false)Long divisionId,
            @RequestParam(required=false)String allocationStatus,@RequestParam(defaultValue="0")int page,
            @RequestParam(defaultValue="25")int size){return ApiResponse.success("HOD workspace",service.workspace(departmentId,search,courseYearId,divisionId,allocationStatus,page,size));}

    @PostMapping("/students/allocate") public ApiResponse<?> allocate(@Valid @RequestBody BulkAllocationRequest request){return ApiResponse.success("Students allocated",Map.of("count",service.bulkAllocate(request)));}
    @PostMapping("/students/auto-allocate") public ApiResponse<?> autoAllocate(@Valid @RequestBody AutomaticAllocationRequest request){return ApiResponse.success("Students distributed",Map.of("count",service.automaticAllocate(request)));}
    @PostMapping("/students/transfer") public ApiResponse<?> transfer(@Valid @RequestBody TransferRequest request){return ApiResponse.success("Students transferred",Map.of("count",service.transfer(request)));}
    @PutMapping("/subjects/{subjectId}/allocation") @PreAuthorize("hasRole('HOD')") public ApiResponse<?> subjectAllocation(@PathVariable Long subjectId,@Valid @RequestBody SubjectAllocationRequest request){service.allocateSubject(subjectId,request);return ApiResponse.success("Subject allocation saved",null);}
    @PutMapping("/divisions/{sectionId}/class-teacher") public ApiResponse<?> classTeacher(@PathVariable Long sectionId,@Valid @RequestBody ClassTeacherRequest request){service.assignClassTeacher(sectionId,request);return ApiResponse.success("Class teacher assigned",null);}
    @PostMapping("/timetables/{id}/review") @PreAuthorize("hasRole('PRINCIPAL')") public ApiResponse<?> review(@PathVariable Long id,@Valid @RequestBody TimetableReviewRequest request){service.reviewTimetable(id,request);return ApiResponse.success("Timetable review saved",null);}
}

package com.collegeerp.erp.academic.controller;

import com.collegeerp.erp.academic.dto.AcademicSessionDtos.*;
import com.collegeerp.erp.academic.service.AcademicSessionService;
import com.collegeerp.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class AcademicSessionController {
    private final AcademicSessionService service;
    public AcademicSessionController(AcademicSessionService service){this.service=service;}

    @GetMapping("/api/academic-sessions/context")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AcademicContext> context(){return ApiResponse.success("Academic context",service.context());}

    @GetMapping("/api/principal/academic-sessions/years")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ApiResponse<List<AcademicYearView>> years(){return ApiResponse.success("Academic years",service.years());}
    @PutMapping("/api/principal/academic-sessions/terms/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ApiResponse<AcademicTermView> updateTerm(@PathVariable Long id,@Valid @RequestBody UpdateAcademicTermRequest request){return ApiResponse.success("Academic term updated",service.updateTerm(id,request));}
    @PostMapping("/api/principal/academic-sessions/terms/{id}/activate")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ApiResponse<AcademicTermView> activateTerm(@PathVariable Long id,
            @Valid @RequestBody(required=false) ActivateAcademicTermRequest request){
        return ApiResponse.success("Academic term activated",
                service.activateTerm(id,request==null?new ActivateAcademicTermRequest(false,null):request));
    }
    @PostMapping("/api/principal/academic-sessions/semesters/configure")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ApiResponse<List<SemesterView>> configure(@Valid @RequestBody ConfigureSemestersRequest request){return ApiResponse.success("Semesters configured",service.configureSemesters(request));}
    @GetMapping("/api/principal/academic-sessions/semesters")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ApiResponse<List<SemesterView>> semesters(@RequestParam Long departmentId){return ApiResponse.success("Semesters",service.semesters(departmentId));}
    @GetMapping("/api/principal/academic-sessions/offerings")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ApiResponse<List<OfferingView>> offerings(@RequestParam Long academicYearId){return ApiResponse.success("Semester offerings",service.offerings(academicYearId));}
    @GetMapping("/api/principal/academic-sessions/rollover/preview")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ApiResponse<RolloverPreview> preview(@RequestParam Long sourceTermId,@RequestParam Long targetTermId){return ApiResponse.success("Semester rollover preview",service.preview(sourceTermId,targetTermId));}
    @PostMapping("/api/principal/academic-sessions/rollover")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ApiResponse<RolloverResult> rollover(@Valid @RequestBody ExecuteRolloverRequest request){return ApiResponse.success("Semester rollover completed",service.execute(request));}
}

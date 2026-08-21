package com.collegeerp.erp.fee.controller;

import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.fee.service.FeeOfficerWorkspaceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/fee-section/workspace") @PreAuthorize("hasRole('FEE_SECTION')")
public class FeeOfficerWorkspaceController {
    private final FeeOfficerWorkspaceService service;public FeeOfficerWorkspaceController(FeeOfficerWorkspaceService s){service=s;}
    @GetMapping public ApiResponse<?> workspace(@RequestParam(required=false)String search,@RequestParam(required=false)Long departmentId,@RequestParam(required=false)String academicYear,@RequestParam(required=false)Long courseYearId,@RequestParam(required=false)String accountStatus,@RequestParam(required=false)String paymentMode,@RequestParam(required=false)String paymentStatus,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){return ApiResponse.success("Fee officer workspace",service.workspace(search,departmentId,academicYear,courseYearId,accountStatus,paymentMode,paymentStatus,page,size));}
    @GetMapping("/accounts/{id}")public ApiResponse<?> account(@PathVariable Long id){return ApiResponse.success("Fee account detail",service.accountDetail(id));}
}

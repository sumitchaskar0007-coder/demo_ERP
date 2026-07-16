package com.jadhavr.erp.timetable.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.timetable.dto.WeeklyTimetableDtos.*;
import com.jadhavr.erp.timetable.service.WeeklyTimetableService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weekly-timetables")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','HOD','CLASS_TEACHER')")
public class WeeklyTimetableController {
    private final WeeklyTimetableService service;
    public WeeklyTimetableController(WeeklyTimetableService service){this.service=service;}
    @GetMapping("/divisions") public ApiResponse<?> divisions(){return ApiResponse.success("Available divisions",service.divisions());}
    @GetMapping("/sections/{sectionId}") public ApiResponse<?> timetable(@PathVariable Long sectionId){return ApiResponse.success("Weekly timetable",service.getOrCreate(sectionId));}
    @PutMapping("/{id}/entries/{day}/{periodId}") public ApiResponse<?> save(@PathVariable Long id,@PathVariable String day,@PathVariable Long periodId,@Valid @RequestBody SaveEntryRequest request){return ApiResponse.success("Lecture saved",service.save(id,day,periodId,request));}
    @DeleteMapping("/{id}/entries/{day}/{periodId}") public ApiResponse<?> delete(@PathVariable Long id,@PathVariable String day,@PathVariable Long periodId){service.delete(id,day,periodId);return ApiResponse.success("Lecture removed",null);}
    @PatchMapping("/{id}/entries/move") public ApiResponse<?> move(@PathVariable Long id,@Valid @RequestBody MoveEntryRequest request){return ApiResponse.success("Lecture moved",service.move(id,request));}
    @PutMapping("/{id}/periods") public ApiResponse<?> periods(@PathVariable Long id,@Valid @RequestBody UpdatePeriodsRequest request){return ApiResponse.success("Period times updated",service.updatePeriods(id,request));}
}

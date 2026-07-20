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
    @PostMapping("/{id}/copy-day") public ApiResponse<?> copyDay(@PathVariable Long id,@Valid @RequestBody CopyDayRequest request){return ApiResponse.success("Day copied",service.copyDay(id,request));}
    @PostMapping("/{id}/copy-timetable") public ApiResponse<?> copyTimetable(@PathVariable Long id,@Valid @RequestBody CopyTimetableRequest request){return ApiResponse.success("Timetable copied",service.copyTimetable(id,request));}
    @PutMapping("/{id}/entries") public ApiResponse<?> replaceEntries(@PathVariable Long id,@Valid @RequestBody ReplaceEntriesRequest request){return ApiResponse.success("Timetable restored",service.replaceEntries(id,request));}
    @PutMapping("/{id}/periods") public ApiResponse<?> periods(@PathVariable Long id,@Valid @RequestBody UpdatePeriodsRequest request){return ApiResponse.success("Period times updated",service.updatePeriods(id,request));}
    @PostMapping("/{id}/submit-review") @PreAuthorize("hasRole('CLASS_TEACHER')") public ApiResponse<?> submitReview(@PathVariable Long id){return ApiResponse.success("Timetable submitted for Principal review",service.submitForReview(id));}
    @PostMapping("/{id}/review") @PreAuthorize("hasRole('PRINCIPAL')") public ApiResponse<?> review(@PathVariable Long id,@Valid @RequestBody ReviewRequest request){return ApiResponse.success("Timetable review saved",service.review(id,request));}
}

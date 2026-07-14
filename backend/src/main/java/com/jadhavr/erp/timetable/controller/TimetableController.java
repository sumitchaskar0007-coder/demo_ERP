package com.jadhavr.erp.timetable.controller;
import com.jadhavr.erp.timetable.dto.TimetableDtos.*; import com.jadhavr.erp.timetable.service.TimetableService; import jakarta.validation.Valid; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/timetables") public class TimetableController {private final TimetableService service;public TimetableController(TimetableService s){service=s;}
 @GetMapping public List<TimetableResponse> list(){return service.list();}
 @PostMapping @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PRINCIPAL','HOD')") public TimetableResponse create(@Valid @RequestBody CreateTimetableRequest r){return service.create(r);}
 @PostMapping("/{id}/entries") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PRINCIPAL','HOD')") public EntryResponse entry(@PathVariable Long id,@Valid @RequestBody EntryRequest r){return service.addEntry(id,r);}
 @PostMapping("/{id}/publish") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PRINCIPAL','HOD')") public TimetableResponse publish(@PathVariable Long id){return service.publish(id);}
 @PostMapping("/{id}/archive") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PRINCIPAL')") public TimetableResponse archive(@PathVariable Long id){return service.archive(id);}
 @PostMapping("/copy") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PRINCIPAL','HOD')") public TimetableResponse copy(@Valid @RequestBody CopyRequest r){return service.copy(r);}
}

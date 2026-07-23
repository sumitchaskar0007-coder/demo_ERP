package com.jadhavr.erp.timetable.controller;
import com.jadhavr.erp.timetable.dto.TimetableDtos.*; import com.jadhavr.erp.timetable.service.TimetableService; import jakarta.validation.Valid; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.time.DayOfWeek; import java.util.*;
@RestController @RequestMapping("/api/timetables") @PreAuthorize("hasRole('PRINCIPAL')") public class TimetableController {private final TimetableService service;public TimetableController(TimetableService s){service=s;}
 @GetMapping public List<TimetableResponse> list(){return service.list();}
 @GetMapping("/search") public List<EntryResponse> search(@RequestParam(required=false)String query,@RequestParam(required=false)String subject,@RequestParam(required=false)String teacher,@RequestParam(required=false)String room,@RequestParam(required=false)String dayOfWeek,@RequestParam(required=false)Long divisionId){DayOfWeek day=dayOfWeek!=null?DayOfWeek.valueOf(dayOfWeek.toUpperCase()):null;return service.search(new SearchRequest(query,subject,teacher,room,day,divisionId));}
 @PostMapping @PreAuthorize("denyAll()") public TimetableResponse create(@Valid @RequestBody CreateTimetableRequest r){return service.create(r);}
  @PostMapping("/{id}/entries") @PreAuthorize("denyAll()") public EntryResponse entry(@PathVariable Long id,@Valid @RequestBody EntryRequest r){return service.addEntry(id,r);}
  @PutMapping("/{id}/entries/{entryId}") @PreAuthorize("denyAll()") public EntryResponse updateEntry(@PathVariable Long id,@PathVariable Long entryId,@Valid @RequestBody UpdateEntryRequest r){return service.updateEntry(id,entryId,r);}
  @DeleteMapping("/{id}/entries/{entryId}") @PreAuthorize("denyAll()") public void deleteEntry(@PathVariable Long id,@PathVariable Long entryId){service.deleteEntry(id,entryId);}
  @PostMapping("/{id}/publish") @PreAuthorize("denyAll()") public TimetableResponse publish(@PathVariable Long id){return service.publish(id);}
  @PostMapping("/{id}/archive") @PreAuthorize("denyAll()") public TimetableResponse archive(@PathVariable Long id){return service.archive(id);}
  @PostMapping("/copy") @PreAuthorize("denyAll()") public TimetableResponse copy(@Valid @RequestBody CopyRequest r){return service.copy(r);}
  @PostMapping("/{id}/copy-day") @PreAuthorize("denyAll()") public TimetableResponse copyDay(@PathVariable Long id,@Valid @RequestBody CopyDayRequest r){return service.copyDay(id,r);}
  @PostMapping("/{id}/copy-week") @PreAuthorize("denyAll()") public TimetableResponse copyWeek(@PathVariable Long id,@Valid @RequestBody CopyWeekRequest r){return service.copyWeek(id,r);}
  @GetMapping("/rooms") public List<Map<String,Object>> rooms(){return service.availableRooms();}
}

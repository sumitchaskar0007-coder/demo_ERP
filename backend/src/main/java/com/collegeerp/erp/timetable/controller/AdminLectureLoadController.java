package com.collegeerp.erp.timetable.controller;

import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.timetable.entity.WeeklyTimetableEntry;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.collegeerp.erp.timetable.service.TeacherTimetableService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/super-admin/lecture-load")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Transactional(readOnly = true)
public class AdminLectureLoadController {
    private final WeeklyTimetableEntryRepository entries;
    private final TeacherTimetableService teacherTimetables;

    public AdminLectureLoadController(WeeklyTimetableEntryRepository entries,
                                      TeacherTimetableService teacherTimetables) {
        this.entries = entries;
        this.teacherTimetables = teacherTimetables;
    }

    public record LectureLoadRow(Long staffId, String employeeCode, String staffName,
            Long collegeId, String collegeName, Long departmentId, String departmentName,
            long weeklyLectures, long weeklyMinutes, long theoryLectures, long labLectures,
            long otherLectures) {}

    @GetMapping
    public ApiResponse<List<LectureLoadRow>> list(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long courseYearId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long staffId) {
        List<LectureLoadRow> result = entries.lectureLoad(collegeId, departmentId, courseYearId, divisionId, staffId)
                .stream().map(row -> new LectureLoadRow(row.getStaffId(), row.getEmployeeCode(), row.getStaffName(),
                        row.getCollegeId(), row.getCollegeName(), row.getDepartmentId(), row.getDepartmentName(),
                        row.getWeeklyLectures(), row.getWeeklyMinutes(), row.getTheoryLectures(),
                        row.getLabLectures(), row.getOtherLectures()))
                .toList();
        return ApiResponse.success("Staff lecture load retrieved", result);
    }

    @GetMapping("/{staffId}/timetable")
    public ApiResponse<?> timetable(@PathVariable Long staffId) {
        return ApiResponse.success("Staff timetable retrieved", teacherTimetables.timetableFor(staffId));
    }

    private static final class MutableLoad {
        private final WeeklyTimetableEntry first;
        private long count, minutes, theory, lab, other;
        private MutableLoad(WeeklyTimetableEntry first) { this.first = first; }
        private void add(WeeklyTimetableEntry entry) {
            count++;
            minutes += Duration.between(entry.getPeriod().getStartTime(), entry.getPeriod().getEndTime()).toMinutes();
            if (entry.getLectureType() == WeeklyTimetableEntry.LectureType.THEORY) theory++;
            else if (entry.getLectureType() == WeeklyTimetableEntry.LectureType.LAB) lab++;
            else other++;
        }
        private LectureLoadRow row() {
            var teacher = first.getTeacher();
            var department = first.getTimetable().getSection().getDepartment();
            var college = first.getTimetable().getCollege();
            return new LectureLoadRow(teacher.getId(), teacher.getEmployeeCode(), teacher.getFullName(),
                    college.getId(), college.getName(), department.getId(), department.getName(),
                    count, minutes, theory, lab, other);
        }
    }
}

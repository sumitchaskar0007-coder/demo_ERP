package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.dto.AcademicDtos.*;
import com.jadhavr.erp.academic.entity.*;

import java.util.List;
import java.util.Map;

public interface AcademicService {
    AcademicClass createClass(CreateClass r);
    List<Map<String, Object>> classes(Long departmentId);
    AcademicClass classStatus(Long id, boolean active);
    Section createSection(CreateSection r);
    List<Map<String, Object>> sections(Long classId);
    Section assignClassTeacher(Long id, Assign r);
    Map<String, Object> assignStudent(Long id, AssignStudent r);
    Map<String, Object> createSubject(CreateSubject r);
    List<Map<String, Object>> subjects(Long classId);
    SubjectTeacherAssignment assignTeacher(Long id, Assign r);
    TimetableEntry createTimetable(CreateTimetable r);
    List<TimetableEntry> timetable(Long sectionId);
    AttendanceSession createAttendance(CreateAttendance r);
    AttendanceSession mark(Long id, Mark r);
    AttendanceSession submit(Long id);
    Map<String, Object> attendanceSummary(Long studentId);
    List<StudentRosterItem> eligibleStudents(Long academicClassId);
    List<StudentRosterItem> sectionStudents(Long sectionId);
    Map<String, Object> classTeacherRoster();
    Map<String, Object> studentClass();
}

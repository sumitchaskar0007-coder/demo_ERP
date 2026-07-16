package com.jadhavr.erp.attendance.entity;

import com.jadhavr.erp.academic.entity.Section;
import com.jadhavr.erp.academic.entity.Subject;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.timetable.entity.WeeklyTimetableEntry;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "weekly_attendance_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_weekly_attendance_lecture_date", columnNames = {"timetable_entry_id", "attendance_date"}),
        indexes = {
                @Index(name = "idx_weekly_attendance_teacher_date", columnList = "teacher_id,attendance_date"),
                @Index(name = "idx_weekly_attendance_section_date", columnList = "section_id,attendance_date")
        })
public class WeeklyAttendanceSession extends BaseAuditEntity {
    public enum Status { DRAFT, SUBMITTED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "college_id", nullable = false) private College college;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "timetable_entry_id", nullable = false) private WeeklyTimetableEntry timetableEntry;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id", nullable = false) private Section section;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subject_id", nullable = false) private Subject subject;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "teacher_id", nullable = false) private StaffProfile teacher;
    @Column(name = "attendance_date", nullable = false) private LocalDate attendanceDate;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;
    @Column(name = "lecture_number", nullable = false) private Integer lectureNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.DRAFT;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;

    public Long getId(){ return id; }
    public College getCollege(){ return college; } public void setCollege(College v){ college=v; }
    public WeeklyTimetableEntry getTimetableEntry(){ return timetableEntry; } public void setTimetableEntry(WeeklyTimetableEntry v){ timetableEntry=v; }
    public Section getSection(){ return section; } public void setSection(Section v){ section=v; }
    public Subject getSubject(){ return subject; } public void setSubject(Subject v){ subject=v; }
    public StaffProfile getTeacher(){ return teacher; } public void setTeacher(StaffProfile v){ teacher=v; }
    public LocalDate getAttendanceDate(){ return attendanceDate; } public void setAttendanceDate(LocalDate v){ attendanceDate=v; }
    public LocalTime getStartTime(){ return startTime; } public void setStartTime(LocalTime v){ startTime=v; }
    public LocalTime getEndTime(){ return endTime; } public void setEndTime(LocalTime v){ endTime=v; }
    public Integer getLectureNumber(){ return lectureNumber; } public void setLectureNumber(Integer v){ lectureNumber=v; }
    public Status getStatus(){ return status; } public void setStatus(Status v){ status=v; }
    public LocalDateTime getSubmittedAt(){ return submittedAt; } public void setSubmittedAt(LocalDateTime v){ submittedAt=v; }
}

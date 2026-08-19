package com.jadhavr.erp.timetable.entity;

import com.jadhavr.erp.academic.entity.Subject;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.staff.entity.StaffProfile;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_substitutions", indexes = {
        @Index(name = "idx_lecture_substitution_original_date", columnList = "original_teacher_id,lecture_date,status"),
        @Index(name = "idx_lecture_substitution_substitute_date", columnList = "substitute_teacher_id,lecture_date,status"),
        @Index(name = "idx_lecture_substitution_college_date", columnList = "college_id,lecture_date,status")
})
public class LectureSubstitution extends BaseAuditEntity {
    public enum Status { ACTIVE, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_entry_id", nullable = false)
    private WeeklyTimetableEntry timetableEntry;

    @Column(name = "lecture_date", nullable = false)
    private LocalDate lectureDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_teacher_id", nullable = false)
    private StaffProfile originalTeacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "substitute_teacher_id", nullable = false)
    private StaffProfile substituteTeacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "substitute_subject_id", nullable = false)
    private Subject substituteSubject;

    @Column(nullable = false, length = 300)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public Long getId() { return id; }
    public College getCollege() { return college; }
    public void setCollege(College value) { college = value; }
    public WeeklyTimetableEntry getTimetableEntry() { return timetableEntry; }
    public void setTimetableEntry(WeeklyTimetableEntry value) { timetableEntry = value; }
    public LocalDate getLectureDate() { return lectureDate; }
    public void setLectureDate(LocalDate value) { lectureDate = value; }
    public StaffProfile getOriginalTeacher() { return originalTeacher; }
    public void setOriginalTeacher(StaffProfile value) { originalTeacher = value; }
    public StaffProfile getSubstituteTeacher() { return substituteTeacher; }
    public void setSubstituteTeacher(StaffProfile value) { substituteTeacher = value; }
    public Subject getSubstituteSubject() { return substituteSubject; }
    public void setSubstituteSubject(Subject value) { substituteSubject = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public Status getStatus() { return status; }
    public void setStatus(Status value) { status = value; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime value) { cancelledAt = value; }
}

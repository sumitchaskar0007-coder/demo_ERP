package com.jadhavr.erp.attendance.entity;

import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.student.entity.StudentProfile;
import jakarta.persistence.*;

@Entity
@Table(name = "weekly_attendance_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_weekly_attendance_student", columnNames = {"session_id", "student_id"}),
        indexes = @Index(name = "idx_weekly_attendance_record_student", columnList = "student_id"))
public class WeeklyAttendanceRecord extends BaseAuditEntity {
    public enum Status { PRESENT, ABSENT, LATE, LEAVE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false) private WeeklyAttendanceSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false) private StudentProfile student;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.PRESENT;
    @Column(length = 500) private String remarks;

    public Long getId(){ return id; }
    public WeeklyAttendanceSession getSession(){ return session; } public void setSession(WeeklyAttendanceSession v){ session=v; }
    public StudentProfile getStudent(){ return student; } public void setStudent(StudentProfile v){ student=v; }
    public Status getStatus(){ return status; } public void setStatus(Status v){ status=v; }
    public String getRemarks(){ return remarks; } public void setRemarks(String v){ remarks=v; }
}

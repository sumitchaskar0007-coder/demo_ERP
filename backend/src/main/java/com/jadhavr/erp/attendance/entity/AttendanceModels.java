package com.jadhavr.erp.attendance.entity;

import com.jadhavr.erp.academic.entity.AcademicModels.TenantEntity;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.timetable.entity.TimetableModels.TimetableEntry;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.*;
import java.time.*;

/** Manual attendance only. There are deliberately no device identifiers or device sources. */
public final class AttendanceModels {
    private AttendanceModels() {}
    public enum SessionType { DAILY, LECTURE }
    public enum SessionStatus { OPEN, SUBMITTED, LOCKED }
    public enum AttendanceStatus { PRESENT, ABSENT, LATE, EXCUSED, HALF_DAY, LEAVE }

    @Entity(name="ManagedAttendanceSession") @Table(name="attendance_sessions", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","timetable_entry_id","session_date"}))
    public static class AttendanceSession extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="timetable_entry_id") private TimetableEntry timetableEntry;
        @Column(name="session_date",nullable=false) private LocalDate sessionDate;
        @Enumerated(EnumType.STRING) @Column(name="session_type",nullable=false,length=20) private SessionType type=SessionType.LECTURE;
        @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SessionStatus status=SessionStatus.OPEN;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="assigned_teacher_id",nullable=false) private User assignedTeacher;
        @Column(name="lock_at",nullable=false) private LocalDateTime lockAt;
        @Column(name="submitted_at") private LocalDateTime submittedAt;
        @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="submitted_by") private User submittedBy;
        public TimetableEntry getTimetableEntry(){return timetableEntry;} public void setTimetableEntry(TimetableEntry v){timetableEntry=v;} public LocalDate getSessionDate(){return sessionDate;} public void setSessionDate(LocalDate v){sessionDate=v;} public SessionType getType(){return type;} public void setType(SessionType v){type=v;} public SessionStatus getStatus(){return status;} public void setStatus(SessionStatus v){status=v;} public User getAssignedTeacher(){return assignedTeacher;} public void setAssignedTeacher(User v){assignedTeacher=v;} public LocalDateTime getLockAt(){return lockAt;} public void setLockAt(LocalDateTime v){lockAt=v;} public LocalDateTime getSubmittedAt(){return submittedAt;} public void setSubmittedAt(LocalDateTime v){submittedAt=v;} public User getSubmittedBy(){return submittedBy;} public void setSubmittedBy(User v){submittedBy=v;}
    }

    @Entity(name="ManagedStudentAttendance") @Table(name="student_attendance", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","session_id","student_id"}))
    public static class StudentAttendance extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="session_id",nullable=false) private AttendanceSession session;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id",nullable=false) private StudentProfile student;
        @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AttendanceStatus status;
        @Column(length=500) private String remarks;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="marked_by",nullable=false) private User markedBy;
        @Column(name="marked_at",nullable=false) private LocalDateTime markedAt;
        public AttendanceSession getSession(){return session;} public void setSession(AttendanceSession v){session=v;} public StudentProfile getStudent(){return student;} public void setStudent(StudentProfile v){student=v;} public AttendanceStatus getStatus(){return status;} public void setStatus(AttendanceStatus v){status=v;} public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;} public User getMarkedBy(){return markedBy;} public void setMarkedBy(User v){markedBy=v;} public LocalDateTime getMarkedAt(){return markedAt;} public void setMarkedAt(LocalDateTime v){markedAt=v;}
    }

    @Entity(name="ManagedAttendanceCorrection") @Table(name="attendance_corrections")
    public static class AttendanceCorrection extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="attendance_id",nullable=false) private StudentAttendance attendance;
        @Enumerated(EnumType.STRING) @Column(name="old_status",nullable=false,length=20) private AttendanceStatus oldStatus;
        @Enumerated(EnumType.STRING) @Column(name="new_status",nullable=false,length=20) private AttendanceStatus newStatus;
        @Column(nullable=false,length=500) private String reason;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="corrected_by",nullable=false) private User correctedBy;
        @Column(name="corrected_at",nullable=false) private LocalDateTime correctedAt;
        public StudentAttendance getAttendance(){return attendance;} public void setAttendance(StudentAttendance v){attendance=v;} public AttendanceStatus getOldStatus(){return oldStatus;} public void setOldStatus(AttendanceStatus v){oldStatus=v;} public AttendanceStatus getNewStatus(){return newStatus;} public void setNewStatus(AttendanceStatus v){newStatus=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;} public User getCorrectedBy(){return correctedBy;} public void setCorrectedBy(User v){correctedBy=v;} public LocalDateTime getCorrectedAt(){return correctedAt;} public void setCorrectedAt(LocalDateTime v){correctedAt=v;}
    }

    @Entity(name="ManagedAttendanceAuditLog") @Table(name="erp_audit_logs",indexes={@Index(name="idx_audit_tenant_time",columnList="college_id,created_at")})
    public static class AuditLog extends TenantEntity {
        @Column(nullable=false,length=80) private String action; @Column(name="entity_type",nullable=false,length=80) private String entityType;
        @Column(name="entity_id") private Long entityId; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="actor_id",nullable=false) private User actor;
        @Column(columnDefinition="text") private String details;
        public String getAction(){return action;} public void setAction(String v){action=v;} public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;} public Long getEntityId(){return entityId;} public void setEntityId(Long v){entityId=v;} public User getActor(){return actor;} public void setActor(User v){actor=v;} public String getDetails(){return details;} public void setDetails(String v){details=v;}
    }
}

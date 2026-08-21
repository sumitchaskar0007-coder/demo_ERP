package com.collegeerp.erp.timetable.entity;

import com.collegeerp.erp.academic.entity.AcademicModels.*;
import com.collegeerp.erp.user.entity.User;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TimetableModels {
    private TimetableModels() {}
    public enum TimetableStatus { DRAFT, PUBLISHED, ARCHIVED }

    @Entity(name="ManagedTimetable") @Table(name="timetables", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","academic_term_id","class_id","section_id","week_start"}))
    public static class Timetable extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="academic_year_id",nullable=false) private AcademicYear academicYear;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="academic_term_id",nullable=false) private AcademicTerm academicTerm;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_id",nullable=false) private AcademicClass academicClass;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="section_id",nullable=false) private Section section;
        @Column(name="week_start",nullable=false) private LocalDate weekStart;
        @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TimetableStatus status=TimetableStatus.DRAFT;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by",nullable=false) private User createdBy;
        @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="published_by") private User publishedBy;
        @Column(name="published_at") private LocalDateTime publishedAt;
        public AcademicYear getAcademicYear(){return academicYear;} public void setAcademicYear(AcademicYear v){academicYear=v;} public AcademicTerm getAcademicTerm(){return academicTerm;} public void setAcademicTerm(AcademicTerm v){academicTerm=v;} public AcademicClass getAcademicClass(){return academicClass;} public void setAcademicClass(AcademicClass v){academicClass=v;} public Section getSection(){return section;} public void setSection(Section v){section=v;} public LocalDate getWeekStart(){return weekStart;} public void setWeekStart(LocalDate v){weekStart=v;} public TimetableStatus getStatus(){return status;} public void setStatus(TimetableStatus v){status=v;} public User getCreatedBy(){return createdBy;} public void setCreatedBy(User v){createdBy=v;} public User getPublishedBy(){return publishedBy;} public void setPublishedBy(User v){publishedBy=v;} public LocalDateTime getPublishedAt(){return publishedAt;} public void setPublishedAt(LocalDateTime v){publishedAt=v;}
    }

    @Entity(name="ManagedTimetableEntry") @Table(name="timetable_entries", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","timetable_id","day_of_week","period_id"}))
    public static class TimetableEntry extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="timetable_id",nullable=false) private Timetable timetable;
        @Enumerated(EnumType.STRING) @Column(name="day_of_week",nullable=false,length=12) private DayOfWeek dayOfWeek;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="period_id",nullable=false) private Period period;
        @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="subject_id") private Subject subject;
        @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="teacher_id") private User teacher;
        @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="room_id") private Room room;
        @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PeriodType type;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by",nullable=false) private User createdBy;
        public Timetable getTimetable(){return timetable;} public void setTimetable(Timetable v){timetable=v;} public DayOfWeek getDayOfWeek(){return dayOfWeek;} public void setDayOfWeek(DayOfWeek v){dayOfWeek=v;} public Period getPeriod(){return period;} public void setPeriod(Period v){period=v;} public Subject getSubject(){return subject;} public void setSubject(Subject v){subject=v;} public User getTeacher(){return teacher;} public void setTeacher(User v){teacher=v;} public Room getRoom(){return room;} public void setRoom(Room v){room=v;} public PeriodType getType(){return type;} public void setType(PeriodType v){type=v;} public User getCreatedBy(){return createdBy;} public void setCreatedBy(User v){createdBy=v;}
    }
}

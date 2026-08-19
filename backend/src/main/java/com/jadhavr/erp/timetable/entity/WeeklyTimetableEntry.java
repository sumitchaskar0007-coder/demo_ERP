package com.jadhavr.erp.timetable.entity;

import com.jadhavr.erp.academic.entity.Subject;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.staff.entity.StaffProfile;
import jakarta.persistence.*;
import java.time.DayOfWeek;

@Entity
@Table(name="weekly_timetable_entries",uniqueConstraints=@UniqueConstraint(name="uk_weekly_entry_cell",columnNames={"timetable_id","day_of_week","period_id"}))
public class WeeklyTimetableEntry extends BaseAuditEntity {
    public enum LectureType { THEORY, LAB, OTHER }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="timetable_id",nullable=false) private WeeklyTimetable timetable;
    @Enumerated(EnumType.STRING) @Column(name="day_of_week",nullable=false,length=12) private DayOfWeek dayOfWeek;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="period_id",nullable=false) private WeeklyPeriod period;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="subject_id",nullable=false) private Subject subject;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="teacher_id",nullable=false) private StaffProfile teacher;
    @Column(length=80) private String room;
    @Enumerated(EnumType.STRING) @Column(name="lecture_type",nullable=false,length=20) private LectureType lectureType;
    @Column(length=500) private String remarks;
    public Long getId(){return id;} public WeeklyTimetable getTimetable(){return timetable;} public void setTimetable(WeeklyTimetable v){timetable=v;}
    public DayOfWeek getDayOfWeek(){return dayOfWeek;} public void setDayOfWeek(DayOfWeek v){dayOfWeek=v;} public WeeklyPeriod getPeriod(){return period;} public void setPeriod(WeeklyPeriod v){period=v;}
    public Subject getSubject(){return subject;} public void setSubject(Subject v){subject=v;} public StaffProfile getTeacher(){return teacher;} public void setTeacher(StaffProfile v){teacher=v;}
    public String getRoom(){return room;} public void setRoom(String v){room=v;} public LectureType getLectureType(){return lectureType;} public void setLectureType(LectureType v){lectureType=v;}
    public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;}
}

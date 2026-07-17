package com.jadhavr.erp.timetable.entity;

import com.jadhavr.erp.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name="weekly_timetable_periods", uniqueConstraints=@UniqueConstraint(name="uk_weekly_period_position",columnNames={"timetable_id","position"}))
public class WeeklyPeriod extends BaseAuditEntity {
    public enum Kind { TEACHING, SHORT_BREAK, LUNCH_BREAK }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="timetable_id",nullable=false) private WeeklyTimetable timetable;
    @Column(nullable=false) private Integer position;
    @Column(nullable=false,length=40) private String label;
    @Column(name="start_time",nullable=false) private LocalTime startTime;
    @Column(name="end_time",nullable=false) private LocalTime endTime;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Kind kind;
    public Long getId(){return id;} public WeeklyTimetable getTimetable(){return timetable;} public void setTimetable(WeeklyTimetable v){timetable=v;}
    public Integer getPosition(){return position;} public void setPosition(Integer v){position=v;} public String getLabel(){return label;} public void setLabel(String v){label=v;}
    public LocalTime getStartTime(){return startTime;} public void setStartTime(LocalTime v){startTime=v;} public LocalTime getEndTime(){return endTime;} public void setEndTime(LocalTime v){endTime=v;}
    public Kind getKind(){return kind;} public void setKind(Kind v){kind=v;}
}

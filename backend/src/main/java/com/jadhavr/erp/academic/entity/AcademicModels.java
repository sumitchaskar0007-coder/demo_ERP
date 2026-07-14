package com.jadhavr.erp.academic.entity;

import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.*;
import java.time.*;

/** Tenant-owned academic master data. College is always assigned server-side. */
public final class AcademicModels {
    private AcademicModels() {}

    @MappedSuperclass
    public abstract static class TenantEntity extends BaseAuditEntity {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) protected Long id;
        @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name="college_id", nullable=false) protected College college;
        public Long getId(){return id;} public College getCollege(){return college;} public void setCollege(College v){college=v;}
    }

    @Entity(name="ManagedAcademicYear") @Table(name="academic_years", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","name"}))
    public static class AcademicYear extends TenantEntity {
        @Column(nullable=false,length=50) private String name;
        @Column(name="start_date",nullable=false) private LocalDate startDate;
        @Column(name="end_date",nullable=false) private LocalDate endDate;
        @Column(nullable=false) private boolean active;
        public String getName(){return name;} public void setName(String v){name=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    }

    @Entity(name="ManagedAcademicTerm") @Table(name="academic_terms", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","academic_year_id","name"}))
    public static class AcademicTerm extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="academic_year_id",nullable=false) private AcademicYear academicYear;
        @Column(nullable=false,length=80) private String name;
        @Column(name="start_date",nullable=false) private LocalDate startDate;
        @Column(name="end_date",nullable=false) private LocalDate endDate;
        public AcademicYear getAcademicYear(){return academicYear;} public void setAcademicYear(AcademicYear v){academicYear=v;} public String getName(){return name;} public void setName(String v){name=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
    }

    @Entity(name="ManagedProgram") @Table(name="academic_programs", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","code"}))
    public static class Program extends TenantEntity {
        @Column(nullable=false,length=30) private String code; @Column(nullable=false,length=120) private String name;
        public String getCode(){return code;} public void setCode(String v){code=v;} public String getName(){return name;} public void setName(String v){name=v;}
    }

    @Entity(name="ManagedSemester") @Table(name="academic_semesters", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","program_id","number"}))
    public static class Semester extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="program_id",nullable=false) private Program program;
        @Column(nullable=false) private Integer number; @Column(nullable=false,length=80) private String name;
        public Program getProgram(){return program;} public void setProgram(Program v){program=v;} public Integer getNumber(){return number;} public void setNumber(Integer v){number=v;} public String getName(){return name;} public void setName(String v){name=v;}
    }

    @Entity(name="ManagedAcademicClass") @Table(name="academic_classes", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","program_id","semester_id","name"}))
    public static class AcademicClass extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="program_id",nullable=false) private Program program;
        @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="semester_id") private Semester semester;
        @Column(nullable=false,length=100) private String name;
        public Program getProgram(){return program;} public void setProgram(Program v){program=v;} public Semester getSemester(){return semester;} public void setSemester(Semester v){semester=v;} public String getName(){return name;} public void setName(String v){name=v;}
    }

    @Entity(name="ManagedSection") @Table(name="academic_sections", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","class_id","name"}))
    public static class Section extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_id",nullable=false) private AcademicClass academicClass;
        @Column(nullable=false,length=40) private String name;
        public AcademicClass getAcademicClass(){return academicClass;} public void setAcademicClass(AcademicClass v){academicClass=v;} public String getName(){return name;} public void setName(String v){name=v;}
    }

    public enum SubjectType { THEORY, PRACTICAL, LAB }
    @Entity(name="ManagedSubject") @Table(name="academic_subjects", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","code"}))
    public static class Subject extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="program_id",nullable=false) private Program program;
        @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="semester_id") private Semester semester;
        @Column(nullable=false,length=30) private String code; @Column(nullable=false,length=140) private String name;
        @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SubjectType type=SubjectType.THEORY;
        public Program getProgram(){return program;} public void setProgram(Program v){program=v;} public Semester getSemester(){return semester;} public void setSemester(Semester v){semester=v;} public String getCode(){return code;} public void setCode(String v){code=v;} public String getName(){return name;} public void setName(String v){name=v;} public SubjectType getType(){return type;} public void setType(SubjectType v){type=v;}
    }

    public enum RoomType { CLASSROOM, LAB, HALL }
    @Entity(name="ManagedRoom") @Table(name="academic_rooms", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","code"}))
    public static class Room extends TenantEntity {
        @Column(nullable=false,length=30) private String code; @Column(nullable=false,length=100) private String name;
        @Column(nullable=false) private Integer capacity; @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private RoomType type=RoomType.CLASSROOM;
        public String getCode(){return code;} public void setCode(String v){code=v;} public String getName(){return name;} public void setName(String v){name=v;} public Integer getCapacity(){return capacity;} public void setCapacity(Integer v){capacity=v;} public RoomType getType(){return type;} public void setType(RoomType v){type=v;}
    }

    public enum PeriodType { LECTURE, PRACTICAL, LAB, BREAK }
    @Entity(name="ManagedPeriod") @Table(name="academic_periods", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","period_number"}))
    public static class Period extends TenantEntity {
        @Column(name="period_number",nullable=false) private Integer periodNumber;
        @Column(name="start_time",nullable=false) private LocalTime startTime; @Column(name="end_time",nullable=false) private LocalTime endTime;
        @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PeriodType type=PeriodType.LECTURE;
        public Integer getPeriodNumber(){return periodNumber;} public void setPeriodNumber(Integer v){periodNumber=v;} public LocalTime getStartTime(){return startTime;} public void setStartTime(LocalTime v){startTime=v;} public LocalTime getEndTime(){return endTime;} public void setEndTime(LocalTime v){endTime=v;} public PeriodType getType(){return type;} public void setType(PeriodType v){type=v;}
    }

    @Entity(name="ManagedWorkingDay") @Table(name="academic_working_days", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","day_of_week"}))
    public static class WorkingDay extends TenantEntity {
        @Enumerated(EnumType.STRING) @Column(name="day_of_week",nullable=false,length=12) private DayOfWeek dayOfWeek; @Column(nullable=false) private boolean working=true;
        public DayOfWeek getDayOfWeek(){return dayOfWeek;} public void setDayOfWeek(DayOfWeek v){dayOfWeek=v;} public boolean isWorking(){return working;} public void setWorking(boolean v){working=v;}
    }

    @Entity(name="ManagedHoliday") @Table(name="academic_holidays", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","holiday_date"}))
    public static class Holiday extends TenantEntity {
        @Column(name="holiday_date",nullable=false) private LocalDate date; @Column(nullable=false,length=160) private String name;
        public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;} public String getName(){return name;} public void setName(String v){name=v;}
    }

    @Entity(name="ManagedTeacherSubjectAssignment") @Table(name="teacher_subject_assignments", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","teacher_id","subject_id","class_id","section_id"}))
    public static class TeacherSubjectAssignment extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="teacher_id",nullable=false) private User teacher;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="subject_id",nullable=false) private Subject subject;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_id",nullable=false) private AcademicClass academicClass;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="section_id",nullable=false) private Section section;
        public User getTeacher(){return teacher;} public void setTeacher(User v){teacher=v;} public Subject getSubject(){return subject;} public void setSubject(Subject v){subject=v;} public AcademicClass getAcademicClass(){return academicClass;} public void setAcademicClass(AcademicClass v){academicClass=v;} public Section getSection(){return section;} public void setSection(Section v){section=v;}
    }

    @Entity(name="ManagedClassTeacherAssignment") @Table(name="class_teacher_assignments", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","academic_year_id","class_id","section_id"}))
    public static class ClassTeacherAssignment extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="academic_year_id",nullable=false) private AcademicYear academicYear;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="teacher_id",nullable=false) private User teacher;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_id",nullable=false) private AcademicClass academicClass;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="section_id",nullable=false) private Section section;
        public AcademicYear getAcademicYear(){return academicYear;} public void setAcademicYear(AcademicYear v){academicYear=v;} public User getTeacher(){return teacher;} public void setTeacher(User v){teacher=v;} public AcademicClass getAcademicClass(){return academicClass;} public void setAcademicClass(AcademicClass v){academicClass=v;} public Section getSection(){return section;} public void setSection(Section v){section=v;}
    }

    @Entity(name="ManagedStudentEnrollment") @Table(name="student_enrollments", uniqueConstraints=@UniqueConstraint(columnNames={"college_id","academic_year_id","student_id"}))
    public static class StudentEnrollment extends TenantEntity {
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="academic_year_id",nullable=false) private AcademicYear academicYear;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id",nullable=false) private StudentProfile student;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_id",nullable=false) private AcademicClass academicClass;
        @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="section_id",nullable=false) private Section section;
        @Column(nullable=false) private boolean active=true;
        public AcademicYear getAcademicYear(){return academicYear;} public void setAcademicYear(AcademicYear v){academicYear=v;} public StudentProfile getStudent(){return student;} public void setStudent(StudentProfile v){student=v;} public AcademicClass getAcademicClass(){return academicClass;} public void setAcademicClass(AcademicClass v){academicClass=v;} public Section getSection(){return section;} public void setSection(Section v){section=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    }
}

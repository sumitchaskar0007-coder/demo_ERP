package com.collegeerp.erp.teacher.service;

import com.collegeerp.erp.academic.entity.*;
import com.collegeerp.erp.academic.enums.*;
import com.collegeerp.erp.academic.repository.*;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.attendance.entity.*;
import com.collegeerp.erp.attendance.repository.*;
import com.collegeerp.erp.notice.dto.NoticeResponse;
import com.collegeerp.erp.notice.service.NoticeService;
import com.collegeerp.erp.staff.entity.StaffProfile;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.teacher.dto.TeacherWorkspaceDtos.*;
import com.collegeerp.erp.teacher.entity.TeacherNotification;
import com.collegeerp.erp.timetable.entity.*;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.collegeerp.erp.timetable.service.EffectiveLectureService;
import com.collegeerp.erp.timetable.service.EffectiveLectureService.EffectiveLecture;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @Transactional(readOnly=true)
public class TeacherWorkspaceService {
    private final StaffProfileRepository staff;private final SectionRepository sections;private final StudentSectionEnrollmentRepository enrollments;
    private final WeeklyTimetableEntryRepository entries;
    private final WeeklyAttendanceSessionRepository sessions;private final WeeklyAttendanceRecordRepository records;
    private final NoticeService notices;private final TeacherNotificationService notificationService;private final StudentProfileRepository students;
    private final EffectiveLectureService effectiveLectures;
    public TeacherWorkspaceService(StaffProfileRepository staff,SectionRepository sections,StudentSectionEnrollmentRepository enrollments,
            WeeklyTimetableEntryRepository entries,WeeklyAttendanceSessionRepository sessions,
            WeeklyAttendanceRecordRepository records,NoticeService notices,TeacherNotificationService notificationService,StudentProfileRepository students,
            EffectiveLectureService effectiveLectures){this.staff=staff;this.sections=sections;this.enrollments=enrollments;this.entries=entries;this.sessions=sessions;this.records=records;this.notices=notices;this.notificationService=notificationService;this.students=students;this.effectiveLectures=effectiveLectures;}

    public Workspace workspace(String search,Integer attendanceBelow,String gender,Long divisionId,int page,int size){
        Context c=context();LocalDate today=LocalDate.now();
        List<WeeklyAttendanceSession> scopedSessions=sessions.findByTeacherIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(c.teacher().getId(),today.minusYears(2),today.plusDays(1));
        if(c.classTeacher()){
            Set<Long> own=c.classSections().stream().map(Section::getId).collect(Collectors.toSet());
            sessions.findByCollegeIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(c.teacher().getCollege().getId(),today.minusYears(2),today.plusDays(1)).stream().filter(s->own.contains(s.getSection().getId())).forEach(s->{if(scopedSessions.stream().noneMatch(x->x.getId().equals(s.getId())))scopedSessions.add(s);});
        }
        Set<Long> sessionIds=scopedSessions.stream().filter(s->s.getStatus()==WeeklyAttendanceSession.Status.SUBMITTED).map(WeeklyAttendanceSession::getId).collect(Collectors.toSet());
        List<WeeklyAttendanceRecord> scopedRecords=sessionIds.isEmpty()?List.of():records.findBySessionIdIn(sessionIds);
        Map<Long,List<WeeklyAttendanceRecord>> byStudent=scopedRecords.stream().collect(Collectors.groupingBy(r->r.getStudent().getId()));
        List<Section> studentDirectorySections=c.classTeacher()?c.classSections():c.sections();
        List<StudentSectionEnrollment> activeEnrollments=studentDirectorySections.stream().flatMap(s->enrollments.findBySectionIdAndStatus(s.getId(),AcademicStatus.ACTIVE).stream()).collect(Collectors.toMap(e->e.getStudent().getId(),Function.identity(),(a,b)->a,LinkedHashMap::new)).values().stream().toList();
        List<StudentRow> all=activeEnrollments.stream().map(e->student(e,byStudent.getOrDefault(e.getStudent().getId(),List.of()))).filter(r->matches(r,search,attendanceBelow,gender,divisionId,activeEnrollments)).sorted(Comparator.comparing(StudentRow::name)).toList();
        int safeSize=Math.min(Math.max(size,1),100),safePage=Math.max(page,0),from=Math.min(safePage*safeSize,all.size()),to=Math.min(from+safeSize,all.size());
        StudentPage studentPage=new StudentPage(all.subList(from,to),all.size(),safePage,(int)Math.ceil(all.size()/(double)safeSize));
        List<StudentRow> allStudentRows=activeEnrollments.stream().map(e->student(e,byStudent.getOrDefault(e.getStudent().getId(),List.of()))).toList();
        List<WeeklyTimetableEntry> teacherEntries=entries.findByTeacherId(c.teacher().getId()).stream().filter(this::approved).toList();
        List<EffectiveLecture> effectiveToday=effectiveLectures.forTeacher(c.teacher(),today);
        List<WeeklyAttendanceSession> todayTeacher=scopedSessions.stream().filter(s->s.getTeacher().getId().equals(c.teacher().getId())&&s.getAttendanceDate().equals(today)).toList();
        long todayLectures=effectiveToday.size();
        long pending=todayLectures-todayTeacher.stream().filter(s->s.getStatus()==WeeklyAttendanceSession.Status.SUBMITTED).map(s->s.getTimetableEntry().getId()).distinct().count();
        long completed=todayTeacher.stream().filter(s->s.getStatus()==WeeklyAttendanceSession.Status.SUBMITTED).count();
        Set<Long> subjectIds=teacherEntries.stream().map(e->e.getSubject().getId()).collect(Collectors.toSet());
        double overall=percentage(scopedRecords);
        Kpis kpis=new Kpis(todayLectures,Math.max(0,pending),completed,subjectIds.size(),c.sections().size(),c.classTeacher()?c.classSections().stream().mapToLong(s->enrollments.countBySectionIdAndStatus(s.getId(),AcademicStatus.ACTIVE)).sum():null,overall);
        AttendanceMetrics metrics=new AttendanceMetrics(periodPercentage(scopedRecords,today,today),periodPercentage(scopedRecords,today.minusDays(6),today),periodPercentage(scopedRecords,today.withDayOfMonth(1),today),overall,allStudentRows.stream().filter(s->s.attendancePercentage()>=90).count(),allStudentRows.stream().filter(s->s.attendancePercentage()>=75&&s.attendancePercentage()<90).count(),allStudentRows.stream().filter(s->s.attendancePercentage()<75).count(),allStudentRows.stream().filter(s->s.attendancePercentage()<60).count());
        List<TeacherNotification> notificationRows=notificationService.inbox(c.teacher().getId());
        List<NoticeResponse> noticeRows=notices.inbox();
        return new Workspace(c.teacher().getFullName(),c.teacher().getEmployeeCode(),c.classTeacher(),SecurityUtils.hasRole("SUBJECT_TEACHER")||!subjectIds.isEmpty(),kpis,
                c.classSections().stream().map(s->classSummary(s,byStudent)).toList(),studentPage,metrics,
                dailyTrend(scopedRecords,today),weeklyTrend(scopedRecords,today),monthlyTrend(scopedRecords,today),
                attention(allStudentRows,byStudent,today),workload(c,teacherEntries,todayLectures,pending),schedule(effectiveToday,todayTeacher),
                noticeRows.stream().map(this::notice).toList(),notificationRows.stream().map(n->new NotificationRow(n.getId(),n.getType(),n.getMessage(),n.getCreatedAt(),n.getReadAt()==null)).toList(),
                activities(c,notificationRows),divisionInsights(c,activeEnrollments,byStudent,scopedRecords,today));
    }

    public List<AttendanceHistoryRow> studentHistory(Long studentId){Context c=context();Set<Long> visible=c.sections().stream().map(Section::getId).collect(Collectors.toSet());boolean assigned=enrollments.findByStudentIdInAndStatus(List.of(studentId),AcademicStatus.ACTIVE).stream().anyMatch(e->visible.contains(e.getSection().getId()));if(!assigned)throw new AccessDeniedException("Student is outside your assigned divisions");return records.findByStudentIdOrderBySessionAttendanceDateDescSessionStartTimeDesc(studentId).stream().filter(r->visible.contains(r.getSession().getSection().getId())).map(r->new AttendanceHistoryRow(r.getSession().getAttendanceDate(),r.getSession().getStartTime()+" - "+r.getSession().getEndTime(),r.getSession().getSubject().getName(),r.getSession().getSection().getName(),r.getStatus().name(),r.getRemarks())).toList();}

    @Transactional
    public StudentRow assignIdentifiers(Long studentId,StudentIdentifierRequest request){
        Context c=context();
        if(!c.classTeacher())throw new AccessDeniedException("Only an assigned class teacher can manage PRN and roll numbers");
        Set<Long> owned=c.classSections().stream().map(Section::getId).collect(Collectors.toSet());
        StudentSectionEnrollment enrollment=enrollments.findByStudentIdInAndStatus(List.of(studentId),AcademicStatus.ACTIVE).stream()
                .filter(e->owned.contains(e.getSection().getId())).findFirst()
                .orElseThrow(()->new AccessDeniedException("Student is outside your assigned class"));
        String prn=request.prn().trim(),roll=request.rollNumber().trim();
        StudentProfile student=enrollment.getStudent();
        if(students.existsByPrnIgnoreCaseAndIdNot(prn,studentId))throw new BadRequestException("This PRN is already assigned to another student");
        if(enrollments.existsBySectionIdAndRollNumberIgnoreCaseAndStatusAndStudentIdNot(enrollment.getSection().getId(),roll,AcademicStatus.ACTIVE,studentId))throw new BadRequestException("This roll number is already used in your class");
        student.setPrn(prn);
        student.setRollNumber(roll);
        enrollment.setRollNumber(roll);
        students.save(student);
        enrollments.save(enrollment);
        return student(enrollment,List.of());
    }

    private Context context(){StaffProfile teacher=staff.findByUserId(SecurityUtils.getCurrentUserId()).orElseThrow(()->new AccessDeniedException("Teacher profile not found"));if(!(SecurityUtils.isPrincipal()||SecurityUtils.hasRole("HOD")||SecurityUtils.hasRole("CLASS_TEACHER")||SecurityUtils.hasRole("SUBJECT_TEACHER")))throw new AccessDeniedException("Teacher dashboard access denied");List<Section> classSections=sections.findByClassTeacherIdAndStatus(teacher.getId(),SectionStatus.ACTIVE);Set<Section> visible=new LinkedHashSet<>(classSections);entries.findByTeacherId(teacher.getId()).stream().filter(this::approved).map(e->e.getTimetable().getSection()).forEach(visible::add);visible.removeIf(s->!s.getCollege().getId().equals(teacher.getCollege().getId())||!teacher.canTeachInDepartment(s.getDepartment().getId()));return new Context(teacher,!classSections.isEmpty(),classSections,List.copyOf(visible));}
    private boolean approved(WeeklyTimetableEntry entry){return entry.getTimetable().getStatus()==WeeklyTimetable.Status.ACTIVE&&entry.getTimetable().getReviewStatus()==WeeklyTimetable.ReviewStatus.APPROVED;}
    private StudentRow student(StudentSectionEnrollment e,List<WeeklyAttendanceRecord> rows){StudentProfile s=e.getStudent();double pct=percentage(rows);String issue=pct<60?"Attendance below 60%":pct<75?"Attendance below 75%":null;return new StudentRow(s.getId(),pending(e.getRollNumber())?null:e.getRollNumber(),s.getFullName(),s.getAdmissionNumber(),s.getPrn(),e.getSection().getName(),s.getGender(),pct,s.getEmail(),s.getStatus().name(),issue);}
    private boolean matches(StudentRow r,String search,Integer below,String gender,Long divisionId,List<StudentSectionEnrollment> es){String q=search==null?"":search.trim().toLowerCase(Locale.ROOT);boolean text=q.isEmpty()||List.of(r.name(),r.admissionNumber(),Optional.ofNullable(r.prn()).orElse(""),Optional.ofNullable(r.rollNumber()).orElse("")).stream().anyMatch(v->v.toLowerCase(Locale.ROOT).contains(q));boolean division=divisionId==null||es.stream().anyMatch(e->e.getStudent().getId().equals(r.id())&&e.getSection().getId().equals(divisionId));return text&&(below==null||r.attendancePercentage()<below)&&(gender==null||gender.isBlank()||r.gender().equalsIgnoreCase(gender))&&division;}
    private ClassSummary classSummary(Section s,Map<Long,List<WeeklyAttendanceRecord>> byStudent){List<StudentSectionEnrollment> es=enrollments.findBySectionIdAndStatus(s.getId(),AcademicStatus.ACTIVE);return new ClassSummary(s.getId(),s.getAcademicClass().getName()+" "+s.getName(),s.getDepartment().getName(),s.getAcademicClass().getName(),s.getName(),s.getAcademicYear(),es.size(),es.stream().filter(e->"MALE".equalsIgnoreCase(e.getStudent().getGender())).count(),es.stream().filter(e->"FEMALE".equalsIgnoreCase(e.getStudent().getGender())).count(),es.stream().mapToDouble(e->percentage(byStudent.getOrDefault(e.getStudent().getId(),List.of()))).average().orElse(0));}
    private List<StudentRow> attention(List<StudentRow> rows,Map<Long,List<WeeklyAttendanceRecord>> byStudent,LocalDate today){return rows.stream().map(r->{List<WeeklyAttendanceRecord>x=byStudent.getOrDefault(r.id(),List.of());String issue=r.attentionIssue();if(issue==null&&x.size()>=3&&x.stream().sorted(Comparator.comparing((WeeklyAttendanceRecord a)->a.getSession().getAttendanceDate()).reversed()).limit(3).allMatch(a->a.getStatus()==WeeklyAttendanceRecord.Status.ABSENT))issue="Absent for 3 consecutive sessions";if(issue==null&&x.stream().map(a->a.getSession().getAttendanceDate()).max(LocalDate::compareTo).orElse(LocalDate.MIN).isBefore(today.minusDays(14)))issue="Attendance not marked recently";return issue==null?null:new StudentRow(r.id(),r.rollNumber(),r.name(),r.admissionNumber(),r.prn(),r.division(),r.gender(),r.attendancePercentage(),r.email(),r.status(),issue);}).filter(Objects::nonNull).sorted(Comparator.comparingDouble(StudentRow::attendancePercentage)).limit(50).toList();}
    private Workload workload(Context c,List<WeeklyTimetableEntry> teacherEntries,long todayLectures,long pending){List<WorkloadPoint> week=Arrays.stream(DayOfWeek.values()).limit(6).map(d->new WorkloadPoint(title(d.name()),teacherEntries.stream().filter(e->e.getDayOfWeek()==d).count())).toList();List<WorkloadPoint> subject=teacherEntries.stream().collect(Collectors.groupingBy(e->e.getSubject().getName(),LinkedHashMap::new,Collectors.counting())).entrySet().stream().map(e->new WorkloadPoint(e.getKey(),e.getValue())).toList();return new Workload(teacherEntries.stream().map(e->e.getSubject().getId()).distinct().count(),c.sections().size(),teacherEntries.size(),todayLectures,Math.max(0,pending),week,subject);}
    private List<ScheduleRow> schedule(List<EffectiveLecture> lectures,List<WeeklyAttendanceSession> todaySessions){LocalTime now=LocalTime.now();Map<Long,WeeklyAttendanceSession> byEntry=todaySessions.stream().collect(Collectors.toMap(s->s.getTimetableEntry().getId(),Function.identity(),(a,b)->a));return lectures.stream().sorted(Comparator.comparing(e->e.entry().getPeriod().getStartTime())).map(lecture->{WeeklyTimetableEntry e=lecture.entry();String state=now.isAfter(e.getPeriod().getEndTime())?"COMPLETED":!now.isBefore(e.getPeriod().getStartTime())?"CURRENT":"UPCOMING";WeeklyAttendanceSession s=byEntry.get(e.getId());return new ScheduleRow(e.getId(),e.getPeriod().getStartTime()+" - "+e.getPeriod().getEndTime(),lecture.subject().getName(),e.getTimetable().getSection().getAcademicClass().getName()+" "+e.getTimetable().getSection().getName(),e.getLectureType().name(),state,s==null?null:s.getId(),"CURRENT".equals(state)&&(s==null||s.getStatus()==WeeklyAttendanceSession.Status.DRAFT),lecture.substituted(),lecture.substituted()?lecture.originalTeacher().getFullName():null);}).toList();}
    private NoticeRow notice(NoticeResponse n){String lower=(n.title()+" "+n.message()).toLowerCase(Locale.ROOT);String priority=lower.contains("urgent")?"URGENT":lower.contains("important")?"HIGH":"NORMAL";return new NoticeRow(n.id(),n.title(),n.createdByName(),n.createdAt(),priority,n.createdAt().isAfter(LocalDateTime.now().minusDays(7)),n.message());}
    private List<ActivityRow> activities(Context c,List<TeacherNotification> notifications){List<ActivityRow>a=new ArrayList<>();notifications.forEach(n->a.add(new ActivityRow(n.getType(),n.getMessage(),n.getCreatedAt())));c.classSections().stream().flatMap(s->enrollments.findBySectionIdAndStatus(s.getId(),AcademicStatus.ACTIVE).stream()).filter(e->e.getEnrolledAt()!=null).forEach(e->a.add(new ActivityRow("STUDENT_ALLOCATED",e.getStudent().getFullName()+" joined "+e.getSection().getName(),e.getEnrolledAt())));return a.stream().sorted(Comparator.comparing(ActivityRow::occurredAt).reversed()).limit(20).toList();}
    private List<DivisionInsight> divisionInsights(Context c,List<StudentSectionEnrollment> es,Map<Long,List<WeeklyAttendanceRecord>> byStudent,List<WeeklyAttendanceRecord> scoped,LocalDate today){return c.sections().stream().map(s->{List<StudentSectionEnrollment> own=es.stream().filter(e->e.getSection().getId().equals(s.getId())).toList();List<Double> pct=own.stream().map(e->percentage(byStudent.getOrDefault(e.getStudent().getId(),List.of()))).toList();List<WeeklyAttendanceRecord> rows=scoped.stream().filter(r->r.getSession().getSection().getId().equals(s.getId())).toList();return new DivisionInsight(s.getId(),s.getAcademicClass().getName()+" "+s.getName(),own.size(),round(pct.stream().mapToDouble(Double::doubleValue).average().orElse(0)),pct.stream().filter(x->x<75).count(),dailyTrend(rows,today).stream().skip(Math.max(0,dailyTrend(rows,today).size()-7)).toList());}).toList();}
    private List<TrendPoint> dailyTrend(List<WeeklyAttendanceRecord> rows,LocalDate today){return java.util.stream.IntStream.rangeClosed(0,29).mapToObj(i->today.minusDays(29-i)).map(d->new TrendPoint(d.getDayOfMonth()+" "+d.getMonth().name().substring(0,3),periodPercentage(rows,d,d))).toList();}
    private List<TrendPoint> weeklyTrend(List<WeeklyAttendanceRecord> rows,LocalDate today){WeekFields wf=WeekFields.ISO;return java.util.stream.IntStream.rangeClosed(0,7).mapToObj(i->today.minusWeeks(7-i)).map(d->{LocalDate start=d.with(wf.dayOfWeek(),1);return new TrendPoint("W"+d.get(wf.weekOfWeekBasedYear()),periodPercentage(rows,start,start.plusDays(6)));}).toList();}
    private List<TrendPoint> monthlyTrend(List<WeeklyAttendanceRecord> rows,LocalDate today){return java.util.stream.IntStream.rangeClosed(0,5).mapToObj(i->YearMonth.from(today).minusMonths(5-i)).map(m->new TrendPoint(title(m.getMonth().name().substring(0,3)),periodPercentage(rows,m.atDay(1),m.atEndOfMonth()))).toList();}
    private double periodPercentage(List<WeeklyAttendanceRecord> rows,LocalDate from,LocalDate to){return percentage(rows.stream().filter(r->{LocalDate d=r.getSession().getAttendanceDate();return !d.isBefore(from)&&!d.isAfter(to);}).toList());}
    private double percentage(List<WeeklyAttendanceRecord> rows){if(rows.isEmpty())return 0;long attended=rows.stream().filter(r->r.getStatus()==WeeklyAttendanceRecord.Status.PRESENT||r.getStatus()==WeeklyAttendanceRecord.Status.LATE).count();return round(attended*100.0/rows.size());}
    private double round(double v){return Math.round(v*10)/10.0;}private boolean pending(String v){return v==null||v.isBlank()||v.startsWith("PENDING-");}private String title(String v){String s=v.toLowerCase(Locale.ROOT);return Character.toUpperCase(s.charAt(0))+s.substring(1);}private record Context(StaffProfile teacher,boolean classTeacher,List<Section> classSections,List<Section> sections){}
}

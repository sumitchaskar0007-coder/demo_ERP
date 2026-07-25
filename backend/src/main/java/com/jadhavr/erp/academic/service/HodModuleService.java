package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.dto.HodModuleDtos.*;
import com.jadhavr.erp.academic.entity.*;
import com.jadhavr.erp.academic.enums.*;
import com.jadhavr.erp.academic.repository.*;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.attendance.entity.WeeklyAttendanceRecord;
import com.jadhavr.erp.attendance.entity.WeeklyAttendanceSession;
import com.jadhavr.erp.attendance.repository.WeeklyAttendanceRecordRepository;
import com.jadhavr.erp.audit.enums.*;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.*;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.enums.*;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import com.jadhavr.erp.timetable.repository.*;
import com.jadhavr.erp.teacher.service.TeacherNotificationService;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.repository.RoleRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class HodModuleService {
    private static final long MAX_WEEKLY_LECTURES=24;
    private static final Set<StaffType> TEACHING_TYPES=EnumSet.of(StaffType.TEACHER,StaffType.CLASS_TEACHER,StaffType.SUBJECT_TEACHER);
    private final StudentProfileRepository students;
    private final StaffProfileRepository staff;
    private final SectionRepository sections;
    private final SubjectRepository subjects;
    private final StudentSectionEnrollmentRepository enrollments;
    private final SubjectTeacherAssignmentRepository subjectAssignments;
    private final WeeklyTimetableRepository timetables;
    private final WeeklyTimetableEntryRepository timetableEntries;
    private final WeeklyAttendanceRecordRepository attendance;
    private final StudentDivisionTransferRepository transfers;
    private final ClassTeacherAssignmentHistoryRepository classTeacherHistory;
    private final AuditLogService audit;
    private final TeacherNotificationService notifications;
    private final AdmissionFormRepository admissions;
    private final RoleRepository roles;
    private final UserRepository users;

    public HodModuleService(StudentProfileRepository students,StaffProfileRepository staff,SectionRepository sections,
            SubjectRepository subjects,StudentSectionEnrollmentRepository enrollments,
            SubjectTeacherAssignmentRepository subjectAssignments,WeeklyTimetableRepository timetables,
            WeeklyTimetableEntryRepository timetableEntries,WeeklyAttendanceRecordRepository attendance,
            StudentDivisionTransferRepository transfers,ClassTeacherAssignmentHistoryRepository classTeacherHistory,
            AuditLogService audit,TeacherNotificationService notifications,AdmissionFormRepository admissions,
            RoleRepository roles,UserRepository users){
        this.students=students;this.staff=staff;this.sections=sections;this.subjects=subjects;this.enrollments=enrollments;
        this.subjectAssignments=subjectAssignments;this.timetables=timetables;this.timetableEntries=timetableEntries;
        this.attendance=attendance;this.transfers=transfers;this.classTeacherHistory=classTeacherHistory;this.audit=audit;this.notifications=notifications;
        this.admissions=admissions;this.roles=roles;this.users=users;
    }

    @Transactional(readOnly=true)
    public WorkspaceResponse workspace(Long requestedDepartmentId,String search,Long courseYearId,Long divisionId,
            String allocationStatus,int page,int size){
        Scope scope=scope(requestedDepartmentId);Long departmentId=scope.departmentId();
        List<Section> divisionRows=sections.findByDepartmentIdAndStatus(departmentId,SectionStatus.ACTIVE);
        Map<Long,Long> allocated=divisionRows.stream().collect(Collectors.toMap(Section::getId,
                s->enrollments.countBySectionIdAndStatus(s.getId(),AcademicStatus.ACTIVE)));
        Map<Long,StudentSectionEnrollment> enrollmentByStudent=enrollments.findBySectionDepartmentIdAndStatus(departmentId,AcademicStatus.ACTIVE)
                .stream().collect(Collectors.toMap(e->e.getStudent().getId(),Function.identity(),(a,b)->a));
        Map<Long,AdmissionForm> admissionByStudent=admissions.findByDepartmentId(departmentId).stream()
                .filter(a->a.getStudent()!=null).collect(Collectors.toMap(a->a.getStudent().getId(),Function.identity(),
                        (a,b)->a.getCreatedAt().isAfter(b.getCreatedAt())?a:b));
        List<StudentRow> departmentStudents=students.findByDepartmentId(departmentId).stream().filter(s->s.getStatus()==StudentStatus.ACTIVE)
                .map(s->studentRow(s,enrollmentByStudent.get(s.getId()),admissionByStudent.get(s.getId()))).toList();
        List<StudentRow> allStudents=departmentStudents.stream()
                .filter(r->matches(r,search,courseYearId,divisionId,allocationStatus)).toList();
        int safeSize=Math.min(Math.max(size,1),100),safePage=Math.max(page,0),from=Math.min(safePage*safeSize,allStudents.size()),to=Math.min(from+safeSize,allStudents.size());
        List<StaffProfile> teachers=staff.findByCollegeId(scope.collegeId()).stream()
                .filter(s->s.getStatus()==StaffStatus.ACTIVE&&s.belongsToDepartment(departmentId)&&TEACHING_TYPES.contains(s.getStaffType())).toList();
        Map<Long,List<SubjectTeacherAssignment>> assignments=subjectAssignments.findBySubjectDepartmentIdAndStatus(departmentId,AcademicStatus.ACTIVE)
                .stream().collect(Collectors.groupingBy(a->a.getSubject().getId()));
        List<WeeklyTimetable> activeTableRows=timetables.findBySectionDepartmentIdAndStatus(departmentId,WeeklyTimetable.Status.ACTIVE);
        Map<Long,WeeklyTimetable> workingTables=timetables.findBySectionDepartmentId(departmentId).stream()
                .filter(table->table.getStatus()!=WeeklyTimetable.Status.ARCHIVED)
                .collect(Collectors.toMap(table->table.getSection().getId(),Function.identity(),
                        (first,second)->first.getStatus()==WeeklyTimetable.Status.DRAFT?first:second));
        List<WeeklyTimetable> tableRows=new ArrayList<>(workingTables.values());
        Summary summary=summary(departmentId,teachers,divisionRows,activeTableRows,departmentStudents.size());
        List<ActivityItem> activity=activity(departmentId);
        List<TeacherRow> eligibleClassTeachers=teachers.stream()
                .filter(this::canBecomeClassTeacher)
                .map(t->teacherRow(t,assignments))
                .toList();
        return new WorkspaceResponse(departmentId,scope.departmentName(),summary,
                divisionRows.stream().map(s->new DivisionCard(s.getId(),s.getAcademicClass().getId(),s.getAcademicClass().getName(),s.getAcademicYear(),s.getName(),s.getCode(),s.getCapacity(),allocated.getOrDefault(s.getId(),0L),s.getClassTeacher()==null?null:s.getClassTeacher().getFullName())).toList(),
                allStudents.subList(from,to),allStudents.size(),safePage,(int)Math.ceil(allStudents.size()/(double)safeSize),
                teachers.stream().map(t->teacherRow(t,assignments)).toList(),
                eligibleClassTeachers,
                subjects.findAll().stream().filter(s->s.getDepartment().getId().equals(departmentId)&&s.getStatus()==SubjectStatus.ACTIVE).map(s->subjectRow(s,assignments.getOrDefault(s.getId(),List.of()))).toList(),
                tableRows.stream().map(this::timetableRow).toList(),activity);
    }

    @Transactional(readOnly=true)
    public Summary dashboard(Long requestedDepartmentId){Scope s=scope(requestedDepartmentId);List<Section>d=sections.findByDepartmentIdAndStatus(s.departmentId(),SectionStatus.ACTIVE);List<StaffProfile>t=staff.findByCollegeId(s.collegeId()).stream().filter(x->x.getStatus()==StaffStatus.ACTIVE&&x.belongsToDepartment(s.departmentId())&&TEACHING_TYPES.contains(x.getStaffType())).toList();long studentCount=students.findByDepartmentId(s.departmentId()).stream().filter(x->x.getStatus()==StudentStatus.ACTIVE).count();return summary(s.departmentId(),t,d,timetables.findBySectionDepartmentIdAndStatus(s.departmentId(),WeeklyTimetable.Status.ACTIVE),studentCount);}

    public int bulkAllocate(BulkAllocationRequest request){Section target=section(request.sectionId());Set<Long> unique=new LinkedHashSet<>(request.studentIds());ensureCapacity(target,unique.size(),Set.of());int count=0;for(Long id:unique){allocate(student(id),target);count++;}notifications.notifyDivision(target,"STUDENT_ALLOCATED",count+" new student(s) added to "+target.getName());audit.log(AuditModule.ACADEMIC,AuditAction.ASSIGN,"Section",target.getId(),"Allocated "+count+" students to "+target.getName());return count;}

    public int automaticAllocate(AutomaticAllocationRequest request){Scope scope=scope(null);List<Section> choices=sections.findByDepartmentIdAndStatus(scope.departmentId(),SectionStatus.ACTIVE).stream().filter(s->s.getAcademicClass().getId().equals(request.courseYearId())).toList();if(choices.isEmpty())throw new BadRequestException("No active divisions exist for this course year");List<StudentProfile> eligible=(request.studentIds()==null||request.studentIds().isEmpty()?students.findByDepartmentId(scope.departmentId()):request.studentIds().stream().distinct().map(this::student).toList()).stream().filter(s->s.getStatus()==StudentStatus.ACTIVE).filter(s->enrollments.findFirstByStudentAndStatus(s,AcademicStatus.ACTIVE).isEmpty()).toList();Map<Long,Long> used=choices.stream().collect(Collectors.toMap(Section::getId,s->enrollments.countBySectionIdAndStatus(s.getId(),AcademicStatus.ACTIVE)));int available=choices.stream().mapToInt(s->Math.max(0,s.getCapacity()-used.get(s.getId()).intValue())).sum();if(eligible.size()>available)throw new BadRequestException("Only "+available+" division seats are available for "+eligible.size()+" students");for(StudentProfile student:eligible){Section target=choices.stream().filter(s->used.get(s.getId())<s.getCapacity()).min(Comparator.comparingLong(s->used.get(s.getId()))).orElseThrow();allocate(student,target);used.put(target.getId(),used.get(target.getId())+1);}if(!eligible.isEmpty())choices.forEach(s->notifications.notifyDivision(s,"STUDENT_ALLOCATED","Student allocation was updated for "+s.getName()));audit.log(AuditModule.ACADEMIC,AuditAction.ASSIGN,"AcademicClass",request.courseYearId(),"Automatically distributed "+eligible.size()+" students evenly");return eligible.size();}

    public int transfer(TransferRequest request){Section target=section(request.targetSectionId());List<StudentSectionEnrollment> moving=new ArrayList<>();for(Long id:new LinkedHashSet<>(request.studentIds())){StudentProfile student=student(id);StudentSectionEnrollment e=enrollments.findFirstByStudentAndStatus(student,AcademicStatus.ACTIVE).orElseThrow(()->new BadRequestException(student.getFullName()+" is not allocated"));if(!e.getAcademicClass().getId().equals(target.getAcademicClass().getId())||!e.getAcademicYear().equals(target.getAcademicYear()))throw new BadRequestException("Transfers must stay in the same course year and academic year");if(e.getSection().getId().equals(target.getId()))throw new BadRequestException(student.getFullName()+" is already in "+target.getName());moving.add(e);}ensureCapacity(target,moving.size(),moving.stream().map(e->e.getStudent().getId()).collect(Collectors.toSet()));StaffProfile actor=currentStaff();for(StudentSectionEnrollment e:moving){Section previous=e.getSection();e.setSection(target);e.setAcademicClass(target.getAcademicClass());enrollments.save(e);StudentDivisionTransfer h=new StudentDivisionTransfer();h.setStudent(e.getStudent());h.setFromSection(previous);h.setToSection(target);h.setChangedBy(actor);transfers.save(h);}notifications.notifyDivision(target,"STUDENT_TRANSFERRED",moving.size()+" student(s) transferred to "+target.getName());audit.log(AuditModule.ACADEMIC,AuditAction.UPDATE,"Section",target.getId(),"Transferred "+moving.size()+" students to "+target.getName()+"; historical attendance preserved");return moving.size();}

    @Transactional(readOnly=true)
    public void allocateSubject(Long subjectId,SubjectAllocationRequest request){Subject subject=subject(subjectId);StaffProfile teacher=teacher(request.teacherId(),subject.getDepartment().getId());List<Section> divisions=request.divisionIds().stream().distinct().map(this::section).toList();for(Section s:divisions)if(!s.getAcademicClass().getId().equals(subject.getAcademicClass().getId())||!s.getAcademicYear().equals(subject.getAcademicYear()))throw new BadRequestException("Every division must match the subject course and academic year");long existing=timetableEntries.findByTeacherId(teacher.getId()).stream().filter(e->e.getTimetable().getStatus()==WeeklyTimetable.Status.ACTIVE&&e.getTimetable().getReviewStatus()==WeeklyTimetable.ReviewStatus.APPROVED).count();long requested=Math.max(1,Optional.ofNullable(subject.getCredits()).orElse(1))*divisions.size();if(existing+requested>MAX_WEEKLY_LECTURES)throw new BadRequestException("Assignment exceeds the "+MAX_WEEKLY_LECTURES+" lecture weekly workload limit");SubjectTeacherAssignment row=subjectAssignments.findBySubjectIdAndTeacherIdAndStatus(subjectId,teacher.getId(),AcademicStatus.ACTIVE).orElseGet(SubjectTeacherAssignment::new);row.setSubject(subject);row.setTeacher(teacher);row.setAcademicYear(subject.getAcademicYear());row.setSections(divisions);subjectAssignments.save(row);notifications.notifyTeacher(teacher,"TEACHING_ASSIGNMENT_UPDATED","You are assigned to "+subject.getName()+" for "+divisions.size()+" division(s)");audit.log(AuditModule.ACADEMIC,AuditAction.ASSIGN,"Subject",subjectId,"Assigned "+teacher.getFullName()+" to "+subject.getName()+" for "+divisions.size()+" divisions");}

    public void assignClassTeacher(Long sectionId,ClassTeacherRequest request){
        Section section=section(sectionId);
        StaffProfile teacher=teacher(request.teacherId(),section.getDepartment().getId());
        if(!canBecomeClassTeacher(teacher)&&
                (section.getClassTeacher()==null||!section.getClassTeacher().getId().equals(teacher.getId()))){
            throw new BadRequestException(teacher.getFullName()+" is already class teacher of another active division");
        }
        StaffProfile previous=section.getClassTeacher();
        addClassTeacherRole(teacher);
        section.setClassTeacher(teacher);
        sections.save(section);
        if(previous!=null&&!previous.getId().equals(teacher.getId()))removeClassTeacherRoleIfUnassigned(previous);
        ClassTeacherAssignmentHistory h=new ClassTeacherAssignmentHistory();h.setSection(section);h.setPreviousTeacher(previous);h.setNewTeacher(teacher);h.setAssignedBy(currentStaff());classTeacherHistory.save(h);
        notifications.notifyTeacher(teacher,"CLASS_TEACHER_ASSIGNMENT_CHANGED","You are now class teacher of "+section.getAcademicClass().getName()+" "+section.getName());
        audit.log(AuditModule.ACADEMIC,AuditAction.ASSIGN,"Section",sectionId,"Assigned "+teacher.getFullName()+" as class teacher of "+section.getName());
    }

    public void reviewTimetable(Long id,TimetableReviewRequest request){WeeklyTimetable table=timetables.findById(id).orElseThrow(()->new ResourceNotFoundException("Weekly timetable not found"));section(table.getSection().getId());if(table.getReviewStatus()!=WeeklyTimetable.ReviewStatus.SUBMITTED)throw new BadRequestException("Only submitted timetables can be reviewed");String action=request.action().toUpperCase(Locale.ROOT);WeeklyTimetable.ReviewStatus status=switch(action){case "APPROVE"->WeeklyTimetable.ReviewStatus.APPROVED;case "REJECT"->WeeklyTimetable.ReviewStatus.REJECTED;case "REQUEST_CHANGES","CHANGES_REQUESTED"->WeeklyTimetable.ReviewStatus.CHANGES_REQUESTED;default->throw new BadRequestException("Action must be APPROVE, REJECT, or REQUEST_CHANGES");};if(status!=WeeklyTimetable.ReviewStatus.APPROVED&&(request.comment()==null||request.comment().isBlank()))throw new BadRequestException("A review comment is required");table.setReviewStatus(status);table.setReviewComment(request.comment()==null?null:request.comment().trim());table.setReviewedAt(LocalDateTime.now());if(status==WeeklyTimetable.ReviewStatus.APPROVED){timetables.findFirstBySectionIdAndStatusOrderByIdDesc(table.getSection().getId(),WeeklyTimetable.Status.ACTIVE).filter(current->!current.getId().equals(table.getId())).ifPresent(current->{current.setStatus(WeeklyTimetable.Status.ARCHIVED);timetables.saveAndFlush(current);});table.setStatus(WeeklyTimetable.Status.ACTIVE);}timetables.save(table);notifications.notifyDivision(table.getSection(),"TIMETABLE_UPDATED","Timetable for "+table.getSection().getName()+" was "+status.name().toLowerCase(Locale.ROOT).replace('_',' '));audit.log(AuditModule.ACADEMIC,status==WeeklyTimetable.ReviewStatus.APPROVED?AuditAction.APPROVE:AuditAction.REJECT,"WeeklyTimetable",id,status.name()+" timetable for "+table.getSection().getName());}

    private void allocate(StudentProfile student,Section target){if(student.getStatus()!=StudentStatus.ACTIVE)throw new BadRequestException(student.getFullName()+" is not active");if(!student.getDepartment().getId().equals(target.getDepartment().getId())||!student.getCollege().getId().equals(target.getCollege().getId()))throw new AccessDeniedException("Student is outside your department");if(enrollments.findFirstByStudentAndStatus(student,AcademicStatus.ACTIVE).isPresent())throw new BadRequestException(student.getFullName()+" is already allocated");AdmissionForm admission=admissions.findTopByStudentIdOrderByCreatedAtDesc(student.getId()).orElseThrow(()->new BadRequestException("Approved admission course is missing for "+student.getFullName()));if(admission.getCourseYear()==null)throw new BadRequestException("Approved admission course is missing for "+student.getFullName());if(!admission.getCourseYear().getId().equals(target.getAcademicClass().getId())||!academicYearKey(admission.getAcademicYear()).equals(academicYearKey(target.getAcademicYear())))throw new BadRequestException(student.getFullName()+" is admitted to "+admission.getCourseYear().getName()+"; select one of its divisions");StudentSectionEnrollment e=new StudentSectionEnrollment();e.setStudent(student);e.setSection(target);e.setAcademicClass(target.getAcademicClass());e.setAcademicYear(target.getAcademicYear());e.setRollNumber(null);enrollments.save(e);}
    private void ensureCapacity(Section section,int incoming,Set<Long> ignoredStudentIds){long current=enrollments.findBySectionIdAndStatus(section.getId(),AcademicStatus.ACTIVE).stream().filter(e->!ignoredStudentIds.contains(e.getStudent().getId())).count();if(current+incoming>section.getCapacity())throw new BadRequestException(section.getName()+" has only "+Math.max(0,section.getCapacity()-current)+" seats available");}
    private StudentRow studentRow(StudentProfile s,StudentSectionEnrollment e,AdmissionForm admission){var course=e!=null?e.getAcademicClass():admission==null?null:admission.getCourseYear();String academicYear=e!=null?e.getAcademicYear():admission==null?null:admission.getAcademicYear();return new StudentRow(s.getId(),null,s.getFullName(),s.getPrn(),s.getAdmissionNumber(),s.getGender(),s.getStatus().name(),e==null?null:e.getId(),course==null?null:course.getId(),course==null?null:course.getName(),academicYear,e==null?null:e.getSection().getId(),e==null?null:e.getSection().getName(),e==null||pending(e.getRollNumber())?null:e.getRollNumber(),e==null?"UNALLOCATED":"ALLOCATED");}
    private boolean matches(StudentRow r,String search,Long courseYearId,Long divisionId,String allocation){String q=search==null?"":search.trim().toLowerCase(Locale.ROOT);return(q.isEmpty()||List.of(r.name(),r.admissionNumber(),Optional.ofNullable(r.rollNumber()).orElse("")).stream().anyMatch(v->v.toLowerCase(Locale.ROOT).contains(q)))&&(courseYearId==null||Objects.equals(courseYearId,r.courseYearId()))&&(divisionId==null||Objects.equals(divisionId,r.divisionId()))&&(allocation==null||allocation.isBlank()||allocation.equalsIgnoreCase(r.allocationStatus()));}
    private TeacherRow teacherRow(StaffProfile t,Map<Long,List<SubjectTeacherAssignment>> bySubject){List<SubjectTeacherAssignment>a=bySubject.values().stream().flatMap(Collection::stream).filter(x->x.getTeacher().getId().equals(t.getId())).toList();long lectures=timetableEntries.findByTeacherId(t.getId()).stream().filter(e->e.getTimetable().getStatus()==WeeklyTimetable.Status.ACTIVE&&e.getTimetable().getReviewStatus()==WeeklyTimetable.ReviewStatus.APPROVED).count();long divisions=a.stream().flatMap(x->x.getSections().stream()).map(Section::getId).distinct().count();return new TeacherRow(t.getId(),t.getEmployeeCode(),t.getFullName(),t.getEmail(),t.getStaffType().name(),a.stream().map(x->x.getSubject().getId()).distinct().count(),divisions,lectures,Math.max(0,MAX_WEEKLY_LECTURES-lectures),lectures>=MAX_WEEKLY_LECTURES?"RED":lectures>=MAX_WEEKLY_LECTURES*.75?"YELLOW":"GREEN");}
    private SubjectRow subjectRow(Subject s,List<SubjectTeacherAssignment>a){return new SubjectRow(s.getId(),s.getCode(),s.getName(),s.getAcademicClass().getId(),s.getAcademicClass().getName(),s.getAcademicYear(),Optional.ofNullable(s.getCredits()).orElse(0),a.stream().map(x->x.getTeacher().getId()).distinct().toList(),a.stream().map(x->x.getTeacher().getFullName()).distinct().toList(),a.stream().flatMap(x->x.getSections().stream()).map(Section::getId).distinct().toList());}
    private TimetableReviewRow timetableRow(WeeklyTimetable t){Section s=t.getSection();return new TimetableReviewRow(t.getId(),s.getId(),s.getName(),s.getAcademicClass().getName(),s.getAcademicYear(),t.getReviewStatus().name(),t.getReviewComment(),t.getSubmittedAt(),t.getReviewedAt(),timetableEntries.findByTimetableId(t.getId()).size());}
    private Summary summary(Long departmentId,List<StaffProfile> teachers,List<Section> divisions,List<WeeklyTimetable> tables,long studentCount){long subjectCount=subjects.findAll().stream().filter(s->s.getDepartment().getId().equals(departmentId)&&s.getStatus()==SubjectStatus.ACTIVE).count();DayOfWeek today=LocalDate.now().getDayOfWeek();long classes=tables.stream().flatMap(t->timetableEntries.findByTimetableId(t.getId()).stream()).filter(e->e.getDayOfWeek()==today).map(e->e.getTimetable().getId()).distinct().count();List<WeeklyAttendanceRecord> records=attendance.findBySessionSectionDepartmentIdAndSessionStatus(departmentId,WeeklyAttendanceSession.Status.SUBMITTED);double average=records.isEmpty()?0:records.stream().filter(r->r.getStatus()==WeeklyAttendanceRecord.Status.PRESENT||r.getStatus()==WeeklyAttendanceRecord.Status.LATE).count()*100.0/records.size();long allocated=enrollments.findBySectionDepartmentIdAndStatus(departmentId,AcademicStatus.ACTIVE).size();long pending=Math.max(0,studentCount-allocated)+tables.stream().filter(t->t.getReviewStatus()==WeeklyTimetable.ReviewStatus.SUBMITTED).count();return new Summary(studentCount,teachers.size(),divisions.size(),subjectCount,classes,Math.round(average*10)/10.0,pending);}
    private List<ActivityItem> activity(Long departmentId){List<ActivityItem> result=new ArrayList<>();transfers.findTop20ByToSectionDepartmentIdOrderByTransferredAtDesc(departmentId).forEach(x->result.add(new ActivityItem("TRANSFER",x.getStudent().getFullName()+" moved to "+x.getToSection().getName(),x.getTransferredAt())));classTeacherHistory.findTop20BySectionDepartmentIdOrderByAssignedAtDesc(departmentId).forEach(x->result.add(new ActivityItem("CLASS_TEACHER",x.getNewTeacher().getFullName()+" assigned to "+x.getSection().getName(),x.getAssignedAt())));return result.stream().sorted(Comparator.comparing(ActivityItem::occurredAt).reversed()).toList();}
    private Scope scope(Long requested){StaffProfile profile=currentStaff();Long assigned=profile.getDepartment()==null?null:profile.getDepartment().getId();Long departmentId;if(SecurityUtils.hasRole("HOD")){if(assigned==null)throw new AccessDeniedException("No department is assigned to this HOD");departmentId=assigned;if(requested!=null&&!requested.equals(assigned))throw new AccessDeniedException("Department is outside your scope");}else{departmentId=requested!=null?requested:assigned;if(departmentId==null)throw new BadRequestException("Select a department");}String name=profile.getDepartments().stream().filter(d->d.getId().equals(departmentId)).map(d->d.getName()).findFirst().orElseGet(()->sections.findByDepartmentIdAndStatus(departmentId,SectionStatus.ACTIVE).stream().findFirst().map(s->s.getDepartment().getName()).orElse("Department"));return new Scope(profile.getCollege().getId(),departmentId,name);}
    private StaffProfile currentStaff(){return staff.findByUserId(SecurityUtils.getCurrentUserId()).orElseThrow(()->new AccessDeniedException("Staff profile is required"));}
    private Section section(Long id){Section s=sections.findById(id).orElseThrow(()->new ResourceNotFoundException("Division not found"));Scope scope=scope(null);if(!s.getDepartment().getId().equals(scope.departmentId())||!s.getCollege().getId().equals(scope.collegeId()))throw new AccessDeniedException("Division is outside your department");if(s.getStatus()!=SectionStatus.ACTIVE)throw new BadRequestException("Division is not active");return s;}
    private StudentProfile student(Long id){StudentProfile s=students.findById(id).orElseThrow(()->new ResourceNotFoundException("Student not found"));Scope scope=scope(null);if(!s.getDepartment().getId().equals(scope.departmentId())||!s.getCollege().getId().equals(scope.collegeId()))throw new AccessDeniedException("Student is outside your department");return s;}
    private Subject subject(Long id){Subject s=subjects.findById(id).orElseThrow(()->new ResourceNotFoundException("Subject not found"));Scope scope=scope(null);if(!s.getDepartment().getId().equals(scope.departmentId()))throw new AccessDeniedException("Subject is outside your department");if(s.getStatus()!=SubjectStatus.ACTIVE)throw new BadRequestException("Subject is not active");return s;}
    private StaffProfile teacher(Long id,Long departmentId){StaffProfile t=staff.findById(id).orElseThrow(()->new ResourceNotFoundException("Teacher not found"));if(t.getStatus()!=StaffStatus.ACTIVE||!TEACHING_TYPES.contains(t.getStaffType())||!t.belongsToDepartment(departmentId))throw new BadRequestException("Teacher is not active in this department");return t;}
    private boolean canBecomeClassTeacher(StaffProfile teacher){
        return (teacher.getStaffType()==StaffType.TEACHER||teacher.getStaffType()==StaffType.SUBJECT_TEACHER)
                &&sections.findByClassTeacherIdAndStatus(teacher.getId(),SectionStatus.ACTIVE).isEmpty();
    }
    private void addClassTeacherRole(StaffProfile teacher){
        if(teacher.getUser().getRoles().stream().anyMatch(role->role.getName()==RoleName.CLASS_TEACHER))return;
        Role role=roles.findByName(RoleName.CLASS_TEACHER)
                .orElseThrow(()->new ResourceNotFoundException("CLASS_TEACHER role not found"));
        Set<Role> updated=new HashSet<>(teacher.getUser().getRoles());updated.add(role);
        teacher.getUser().setRoles(updated);users.save(teacher.getUser());
    }
    private void removeClassTeacherRoleIfUnassigned(StaffProfile teacher){
        if(!sections.findByClassTeacherIdAndStatus(teacher.getId(),SectionStatus.ACTIVE).isEmpty())return;
        Set<Role> updated=teacher.getUser().getRoles().stream()
                .filter(role->role.getName()!=RoleName.CLASS_TEACHER)
                .collect(Collectors.toCollection(HashSet::new));
        teacher.getUser().setRoles(updated);users.save(teacher.getUser());
    }
    private boolean pending(String roll){return roll==null||roll.isBlank()||roll.startsWith("PENDING-");}
    private String academicYearKey(String value){return value==null?"":value.replaceAll("[^0-9]","");}
    private record Scope(Long collegeId,Long departmentId,String departmentName){}
}

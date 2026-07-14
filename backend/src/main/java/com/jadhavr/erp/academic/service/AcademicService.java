package com.jadhavr.erp.academic.service;
import com.jadhavr.erp.academic.dto.AcademicDtos.*;
import com.jadhavr.erp.academic.entity.AcademicModels.*;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.exception.*;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek; import java.util.*;

@Service
public class AcademicService {
 private final EntityManager em;
 public AcademicService(EntityManager em){this.em=em;}
 private Long tenant(){Long id=SecurityUtils.requireCurrentUser().getCollegeId();if(id==null)throw new BadRequestException("Select or assign an institution before using academics");return id;}
 private College college(){return em.getReference(College.class,tenant());}
 private <T extends TenantEntity>T owned(Class<T> type,Long id){T value=em.find(type,id);if(value==null||!tenant().equals(value.getCollege().getId()))throw new ResourceNotFoundException(type.getSimpleName()+" not found");return value;}

 @Transactional public MasterResponse create(MasterRequest r){
  String t=r.type().trim().toUpperCase(Locale.ROOT); TenantEntity e;
  switch(t){
   case "ACADEMIC_YEAR"->{AcademicYear x=new AcademicYear();x.setName(r.name());x.setStartDate(req(r.startDate(),"startDate"));x.setEndDate(req(r.endDate(),"endDate"));if(x.getEndDate().isBefore(x.getStartDate()))throw new BadRequestException("End date must be after start date");x.setActive(Boolean.TRUE.equals(r.active()));e=x;}
   case "TERM"->{AcademicTerm x=new AcademicTerm();x.setName(r.name());x.setAcademicYear(owned(AcademicYear.class,req(r.parentId(),"academicYearId")));x.setStartDate(req(r.startDate(),"startDate"));x.setEndDate(req(r.endDate(),"endDate"));e=x;}
   case "PROGRAM"->{Program x=new Program();x.setName(r.name());x.setCode(reqText(r.code(),"code"));e=x;}
   case "SEMESTER"->{Semester x=new Semester();x.setName(r.name());x.setNumber(req(r.number(),"number"));x.setProgram(owned(Program.class,req(r.parentId(),"programId")));e=x;}
   case "CLASS"->{AcademicClass x=new AcademicClass();x.setName(r.name());x.setProgram(owned(Program.class,req(r.parentId(),"programId")));if(r.secondaryParentId()!=null)x.setSemester(owned(Semester.class,r.secondaryParentId()));e=x;}
   case "SECTION"->{Section x=new Section();x.setName(r.name());x.setAcademicClass(owned(AcademicClass.class,req(r.parentId(),"classId")));e=x;}
   case "SUBJECT"->{Subject x=new Subject();x.setName(r.name());x.setCode(reqText(r.code(),"code"));x.setProgram(owned(Program.class,req(r.parentId(),"programId")));if(r.secondaryParentId()!=null)x.setSemester(owned(Semester.class,r.secondaryParentId()));x.setType(SubjectType.valueOf(or(r.category(),"THEORY")));e=x;}
   case "ROOM"->{Room x=new Room();x.setName(r.name());x.setCode(reqText(r.code(),"code"));x.setCapacity(req(r.capacity(),"capacity"));x.setType(RoomType.valueOf(or(r.category(),"CLASSROOM")));e=x;}
   case "PERIOD"->{Period x=new Period();x.setPeriodNumber(req(r.number(),"number"));x.setStartTime(req(r.startTime(),"startTime"));x.setEndTime(req(r.endTime(),"endTime"));if(!x.getEndTime().isAfter(x.getStartTime()))throw new BadRequestException("Period end time must be later");x.setType(PeriodType.valueOf(or(r.category(),"LECTURE")));e=x;}
   case "WORKING_DAY"->{WorkingDay x=new WorkingDay();x.setDayOfWeek(DayOfWeek.valueOf(r.name().toUpperCase(Locale.ROOT)));x.setWorking(!Boolean.FALSE.equals(r.active()));e=x;}
   case "HOLIDAY"->{Holiday x=new Holiday();x.setName(r.name());x.setDate(req(r.startDate(),"date"));e=x;}
   default->throw new BadRequestException("Unsupported academic resource type");
  }
  e.setCollege(college());em.persist(e);em.flush();return response(t,e);
 }

 public List<MasterResponse> list(String type){String t=type.toUpperCase(Locale.ROOT);Class<? extends TenantEntity> c=classFor(t);return em.createQuery("select e from "+c.getSimpleName()+" e where e.college.id=:tenant order by e.id",c).setParameter("tenant",tenant()).getResultList().stream().map(e->response(t,e)).toList();}
 public List<MasterResponse> people(String kind){if("STUDENTS".equalsIgnoreCase(kind))return em.createQuery("select s from StudentProfile s where s.college.id=:c order by s.fullName",StudentProfile.class).setParameter("c",tenant()).getResultList().stream().map(s->new MasterResponse(s.getId(),"STUDENT",s.getFullName(),s.getAdmissionNumber(),s.getEmail())).toList();return em.createQuery("select distinct u from User u join u.roles r where u.college.id=:c and r.name in :roles order by u.fullName",User.class).setParameter("c",tenant()).setParameter("roles",List.of(com.jadhavr.erp.user.entity.RoleName.HOD,com.jadhavr.erp.user.entity.RoleName.CLASS_TEACHER,com.jadhavr.erp.user.entity.RoleName.SUBJECT_TEACHER,com.jadhavr.erp.user.entity.RoleName.PRINCIPAL)).getResultList().stream().map(u->new MasterResponse(u.getId(),"TEACHER",u.getFullName(),u.getEmail(),"Faculty")).toList();}

 @Transactional public MasterResponse assign(AssignmentRequest r){
  User teacher=em.find(User.class,r.teacherId());if(teacher==null||teacher.getCollege()==null||!tenant().equals(teacher.getCollege().getId()))throw new ResourceNotFoundException("Teacher not found");
  if("CLASS_TEACHER".equalsIgnoreCase(r.type())){ClassTeacherAssignment x=new ClassTeacherAssignment();x.setCollege(college());x.setTeacher(teacher);x.setAcademicYear(owned(AcademicYear.class,req(r.academicYearId(),"academicYearId")));x.setAcademicClass(owned(AcademicClass.class,r.classId()));x.setSection(owned(Section.class,r.sectionId()));em.persist(x);em.flush();return new MasterResponse(x.getId(),"CLASS_TEACHER",teacher.getFullName(),null,"Assigned");}
  TeacherSubjectAssignment x=new TeacherSubjectAssignment();x.setCollege(college());x.setTeacher(teacher);x.setSubject(owned(Subject.class,req(r.subjectId(),"subjectId")));x.setAcademicClass(owned(AcademicClass.class,r.classId()));x.setSection(owned(Section.class,r.sectionId()));em.persist(x);em.flush();return new MasterResponse(x.getId(),"SUBJECT_TEACHER",teacher.getFullName(),x.getSubject().getCode(),"Assigned");
 }

 @Transactional public MasterResponse enroll(EnrollmentRequest r){StudentProfile s=em.find(StudentProfile.class,r.studentId());if(s==null||!tenant().equals(s.getCollege().getId()))throw new ResourceNotFoundException("Student not found");StudentEnrollment x=new StudentEnrollment();x.setCollege(college());x.setAcademicYear(owned(AcademicYear.class,r.academicYearId()));x.setStudent(s);x.setAcademicClass(owned(AcademicClass.class,r.classId()));x.setSection(owned(Section.class,r.sectionId()));em.persist(x);em.flush();return new MasterResponse(x.getId(),"ENROLLMENT",s.getFullName(),s.getAdmissionNumber(),"Active");}

 private Class<? extends TenantEntity> classFor(String t){return switch(t){case"ACADEMIC_YEAR"->AcademicYear.class;case"TERM"->AcademicTerm.class;case"PROGRAM"->Program.class;case"SEMESTER"->Semester.class;case"CLASS"->AcademicClass.class;case"SECTION"->Section.class;case"SUBJECT"->Subject.class;case"ROOM"->Room.class;case"PERIOD"->Period.class;case"WORKING_DAY"->WorkingDay.class;case"HOLIDAY"->Holiday.class;default->throw new BadRequestException("Unsupported academic resource type");};}
 private MasterResponse response(String t,TenantEntity e){String name;String code=null;String details="";if(e instanceof AcademicYear x){name=x.getName();details=x.getStartDate()+" to "+x.getEndDate();}else if(e instanceof AcademicTerm x){name=x.getName();details=x.getStartDate()+" to "+x.getEndDate();}else if(e instanceof Program x){name=x.getName();code=x.getCode();}else if(e instanceof Semester x){name=x.getName();details="Semester "+x.getNumber();}else if(e instanceof AcademicClass x)name=x.getName();else if(e instanceof Section x)name=x.getName();else if(e instanceof Subject x){name=x.getName();code=x.getCode();details=x.getType().name();}else if(e instanceof Room x){name=x.getName();code=x.getCode();details=x.getType()+" · "+x.getCapacity();}else if(e instanceof Period x){name="Period "+x.getPeriodNumber();details=x.getStartTime()+"–"+x.getEndTime()+" · "+x.getType();}else if(e instanceof WorkingDay x){name=x.getDayOfWeek().name();details=x.isWorking()?"Working":"Closed";}else {Holiday x=(Holiday)e;name=x.getName();details=x.getDate().toString();}return new MasterResponse(e.getId(),t,name,code,details);}
 private static <T>T req(T v,String n){if(v==null)throw new BadRequestException(n+" is required");return v;} private static String reqText(String v,String n){if(v==null||v.isBlank())throw new BadRequestException(n+" is required");return v.trim();} private static String or(String v,String d){return v==null||v.isBlank()?d:v.toUpperCase(Locale.ROOT);}
}

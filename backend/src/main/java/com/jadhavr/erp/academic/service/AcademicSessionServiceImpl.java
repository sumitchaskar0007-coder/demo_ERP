package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.dto.AcademicSessionDtos.*;
import com.jadhavr.erp.academic.entity.*;
import com.jadhavr.erp.academic.entity.AcademicModels.AcademicTerm;
import com.jadhavr.erp.academic.entity.AcademicModels.AcademicYear;
import com.jadhavr.erp.academic.enums.*;
import com.jadhavr.erp.academic.repository.*;
import com.jadhavr.erp.audit.enums.*;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.*;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AcademicSessionServiceImpl implements AcademicSessionService {
    private final AcademicYearRepository years;
    private final AcademicTermRepository terms;
    private final CurriculumSemesterRepository semesters;
    private final SemesterOfferingRepository offerings;
    private final StudentSectionEnrollmentRepository enrollments;
    private final SectionRepository sections;
    private final DepartmentRepository departments;
    private final CollegeRepository colleges;
    private final UserRepository users;
    private final SemesterRolloverJobRepository jobs;
    private final SemesterRolloverItemRepository items;
    private final AuditLogService audit;
    private final WeeklyTimetableRepository timetables;
    private final Clock clock;

    @Autowired
    public AcademicSessionServiceImpl(AcademicYearRepository years, AcademicTermRepository terms,
            CurriculumSemesterRepository semesters, SemesterOfferingRepository offerings,
            StudentSectionEnrollmentRepository enrollments, SectionRepository sections,
            DepartmentRepository departments, CollegeRepository colleges, UserRepository users,
            SemesterRolloverJobRepository jobs, SemesterRolloverItemRepository items,
            AuditLogService audit, WeeklyTimetableRepository timetables) {
        this(years,terms,semesters,offerings,enrollments,sections,departments,colleges,users,
                jobs,items,audit,timetables,Clock.systemDefaultZone());
    }

    AcademicSessionServiceImpl(AcademicYearRepository years, AcademicTermRepository terms,
            CurriculumSemesterRepository semesters, SemesterOfferingRepository offerings,
            StudentSectionEnrollmentRepository enrollments, SectionRepository sections,
            DepartmentRepository departments, CollegeRepository colleges, UserRepository users,
            SemesterRolloverJobRepository jobs, SemesterRolloverItemRepository items,
            AuditLogService audit, WeeklyTimetableRepository timetables, Clock clock) {
        this.years=years; this.terms=terms; this.semesters=semesters; this.offerings=offerings;
        this.enrollments=enrollments; this.sections=sections; this.departments=departments;
        this.colleges=colleges; this.users=users; this.jobs=jobs; this.items=items; this.audit=audit;
        this.timetables=timetables; this.clock=clock;
    }

    @Override @Transactional(readOnly=true)
    public AcademicContext context() {
        Long collegeId=collegeId();
        AcademicYear year=years.findByCollegeIdAndStatus(collegeId,AcademicYearStatus.ACTIVE).orElse(null);
        AcademicTerm term=terms.findByCollegeIdAndStatus(collegeId,AcademicTermStatus.ACTIVE).orElse(null);
        if(year==null||term==null||!term.getAcademicYear().getId().equals(year.getId())) {
            return new AcademicContext(year==null?null:year.getId(),year==null?null:year.getName(),
                    null,null,null,null,null,false);
        }
        return new AcademicContext(year.getId(),year.getName(),term.getId(),term.getName(),
                term.getTermType(),term.getStartDate(),term.getEndDate(),
                !LocalDate.now(clock).isBefore(term.getEndDate().minusDays(14)));
    }

    @Override @Transactional(readOnly=true)
    public List<AcademicYearView> years(){return years.findByCollegeIdOrderByStartDateDesc(collegeId()).stream().map(this::yearView).toList();}

    @Override
    public AcademicYearView createYear(CreateAcademicYearRequest request) {
        Long collegeId=collegeId(); validateYearDates(request.startDate(),request.endDate());
        validateTermDates(request.startDate(),request.endDate(),request.oddTerm(),request.evenTerm());
        String name=normalizeYear(request.name());
        if(years.existsByCollegeIdAndNameIgnoreCase(collegeId,name)) throw new DuplicateResourceException("Academic year already exists");
        College college=colleges.findById(collegeId).orElseThrow(()->new ResourceNotFoundException("College not found"));
        AcademicYear year=new AcademicYear(); year.setCollege(college); year.setName(name);
        year.setStartDate(request.startDate()); year.setEndDate(request.endDate()); year.setStatus(AcademicYearStatus.DRAFT);
        year=years.save(year);
        createTerm(year,AcademicTermType.ODD,"Odd Semester",request.oddTerm());
        createTerm(year,AcademicTermType.EVEN,"Even Semester",request.evenTerm());
        createOfferings(year);
        audit.log(AuditModule.ACADEMIC,AuditAction.CREATE,"AcademicYear",year.getId(),"Created academic year "+name+" with odd and even semester calendars");
        return yearView(year);
    }

    @Override
    public AcademicYearView updateYear(Long id,UpdateAcademicYearRequest request){
        AcademicYear year=year(id); if(year.getStatus()==AcademicYearStatus.CLOSED) throw new BadRequestException("Closed academic years cannot be edited");
        validateYearDates(request.startDate(),request.endDate()); String name=normalizeYear(request.name());
        if(!year.getName().equalsIgnoreCase(name)&&years.existsByCollegeIdAndNameIgnoreCase(collegeId(),name)) throw new DuplicateResourceException("Academic year already exists");
        for(AcademicTerm term:terms.findByAcademicYearIdOrderByStartDate(id)) if(term.getStartDate().isBefore(request.startDate())||term.getEndDate().isAfter(request.endDate())) throw new BadRequestException("Term dates must remain inside the academic year");
        year.setName(name);year.setStartDate(request.startDate());year.setEndDate(request.endDate());
        audit.log(AuditModule.ACADEMIC,AuditAction.UPDATE,"AcademicYear",id,"Updated academic year calendar "+name);return yearView(year);
    }

    @Override
    public AcademicYearView activateYear(Long id){AcademicYear target=year(id);years.findByCollegeIdAndStatus(collegeId(),AcademicYearStatus.ACTIVE).filter(x->!x.getId().equals(id)).ifPresent(current->current.setStatus(current.getEndDate().isBefore(target.getStartDate())?AcademicYearStatus.CLOSED:AcademicYearStatus.DRAFT));target.setStatus(AcademicYearStatus.ACTIVE);audit.log(AuditModule.ACADEMIC,AuditAction.ACTIVATE,"AcademicYear",id,"Activated academic year "+target.getName());return yearView(target);}

    @Override
    public AcademicTermView updateTerm(Long id,UpdateAcademicTermRequest request){AcademicTerm term=term(id);if(term.getStatus()==AcademicTermStatus.CLOSED)throw new BadRequestException("Closed terms cannot be edited");if(request.startDate().isAfter(request.endDate()))throw new BadRequestException("Term start date must be on or before its end date");AcademicYear year=term.getAcademicYear();if(request.startDate().isBefore(year.getStartDate())||request.endDate().isAfter(year.getEndDate()))throw new BadRequestException("Term dates must be inside the academic year");AcademicTerm other=terms.findByAcademicYearIdOrderByStartDate(year.getId()).stream().filter(x->!x.getId().equals(id)).findFirst().orElse(null);if(other!=null&&overlaps(request.startDate(),request.endDate(),other.getStartDate(),other.getEndDate()))throw new BadRequestException("Odd and even term dates cannot overlap");term.setName(request.name().trim());term.setStartDate(request.startDate());term.setEndDate(request.endDate());audit.log(AuditModule.ACADEMIC,AuditAction.UPDATE,"AcademicTerm",id,"Updated "+term.getTermType()+" semester dates");return termView(term);}

    @Override
    public AcademicTermView activateTerm(Long id,ActivateAcademicTermRequest request){
        AcademicTerm target=term(id);AcademicYear year=target.getAcademicYear();
        if(year.getStatus()!=AcademicYearStatus.ACTIVE)throw new BadRequestException("Activate the academic year before activating its term");
        LocalDate today=LocalDate.now(clock);
        Optional<AcademicTerm> current=terms.findByCollegeIdAndStatus(collegeId(),AcademicTermStatus.ACTIVE)
                .filter(active->!active.getId().equals(target.getId()));
        boolean outsideWindow=!within(today,target);
        boolean bypassesRollover=current.filter(active->target.getStartDate().isAfter(active.getStartDate()))
                .filter(active->enrollments.existsBySemesterOfferingAcademicTermIdAndStatus(active.getId(),AcademicStatus.ACTIVE))
                .isPresent();
        boolean override=request!=null&&request.overrideDate();
        if((outsideWindow||bypassesRollover)&&!override){
            if(outsideWindow)throw new BadRequestException(target.getName()+" cannot be activated on "+today+" because it runs from "+target.getStartDate()+" to "+target.getEndDate()+". Use an authorized override only for an exceptional correction");
            throw new BadRequestException("Active students must be moved using semester rollover. Use an authorized override only for an exceptional correction");
        }
        String reason=null;
        if(override){reason=request.reason()==null?null:request.reason().trim();if(reason==null||reason.length()<10)throw new BadRequestException("An override reason of at least 10 characters is required");}
        activateTermInternal(target);
        String detail="Activated "+target.getTermType()+" semester for "+year.getName();
        if(override)detail+=" using authorized override. Reason: "+reason;
        audit.log(AuditModule.ACADEMIC,AuditAction.ACTIVATE,"AcademicTerm",id,detail);
        return termView(target);
    }

    @Override
    public List<SemesterView> configureSemesters(ConfigureSemestersRequest request){Department department=department(request.departmentId());int max=request.durationYears()*2;semesters.findByDepartmentIdOrderBySemesterNumber(department.getId()).stream().filter(semester->semester.getSemesterNumber()>max).forEach(semester->semester.setActive(false));for(int number=1;number<=max;number++){CurriculumSemester semester=semesters.findByDepartmentIdAndSemesterNumber(department.getId(),number).orElseGet(CurriculumSemester::new);semester.setCollege(department.getCollege());semester.setDepartment(department);semester.setSemesterNumber(number);semester.setYearName(yearName(number));semester.setTermType(number%2==1?AcademicTermType.ODD:AcademicTermType.EVEN);semester.setName("Semester "+number);semester.setActive(true);semesters.save(semester);}for(AcademicYear year:years.findByCollegeIdOrderByStartDateDesc(collegeId()))createOfferings(year);audit.log(AuditModule.ACADEMIC,AuditAction.UPDATE,"Department",department.getId(),"Configured "+max+" semesters for "+department.getName());return semesters(department.getId());}

    @Override @Transactional(readOnly=true)
    public List<SemesterView> semesters(Long departmentId){department(departmentId);return semesters.findByDepartmentIdOrderBySemesterNumber(departmentId).stream().map(this::semesterView).toList();}
    @Override @Transactional(readOnly=true)
    public List<OfferingView> offerings(Long academicYearId){year(academicYearId);return offerings.findByAcademicYearIdOrderByDepartmentIdAscCurriculumSemesterSemesterNumberAsc(academicYearId).stream().map(this::offeringView).toList();}

    @Override @Transactional(readOnly=true)
    public RolloverPreview preview(Long sourceTermId,Long targetTermId){AcademicTerm source=term(sourceTermId),target=term(targetTermId);validateTransition(source,target);List<RolloverStudentPreview> rows=new ArrayList<>();int promote=0,graduate=0,blocked=0;for(StudentSectionEnrollment enrollment:enrollments.findBySemesterOfferingAcademicTermIdAndStatus(sourceTermId,AcademicStatus.ACTIVE)){CurriculumSemester current=enrollment.getSemesterOffering().getCurriculumSemester();Optional<CurriculumSemester> next=semesters.findByDepartmentIdAndSemesterNumber(current.getDepartment().getId(),current.getSemesterNumber()+1).filter(CurriculumSemester::isActive);if(next.isEmpty()){graduate++;rows.add(previewRow(enrollment,null,"GRADUATE","Final configured semester completed"));continue;}Optional<SemesterOffering> offering=offerings.findByAcademicTermIdAndCurriculumSemesterId(targetTermId,next.get().getId());if(offering.isEmpty()){blocked++;rows.add(previewRow(enrollment,next.get().getSemesterNumber(),"BLOCKED","Target semester offering is not configured"));continue;}if(!source.getAcademicYear().getId().equals(target.getAcademicYear().getId())&&autoTargetSection(enrollment,offering.get(),target).isEmpty()){blocked++;rows.add(previewRow(enrollment,next.get().getSemesterNumber(),"BLOCKED","Create the matching "+next.get().getYearName().name().toLowerCase().replace('_',' ')+" division "+enrollment.getSection().getCode()+" for "+target.getAcademicYear().getName()));}else{promote++;rows.add(previewRow(enrollment,next.get().getSemesterNumber(),"PROMOTE",null));}}boolean mapping=!source.getAcademicYear().getId().equals(target.getAcademicYear().getId());return new RolloverPreview(sourceTermId,targetTermId,rows.size(),promote,graduate,blocked,mapping,rows);}

    @Override
    public RolloverResult execute(ExecuteRolloverRequest request){if(!"PROMOTE".equals(request.confirmation()))throw new BadRequestException("Type PROMOTE to confirm the semester rollover");AcademicTerm source=term(request.sourceTermId()),target=term(request.targetTermId());validateTransition(source,target);if(source.getStatus()!=AcademicTermStatus.ACTIVE)throw new BadRequestException("The source semester must be active before rollover");LocalDate today=LocalDate.now(clock);if(today.isBefore(target.getStartDate()))throw new BadRequestException("Semester rollover cannot run before the target semester starts on "+target.getStartDate());jobs.findByCollegeIdAndSourceTermIdAndTargetTermId(collegeId(),source.getId(),target.getId()).ifPresent(existing->{throw new DuplicateResourceException("This semester rollover has already been executed or started");});List<StudentSectionEnrollment> sourceRows=enrollments.findBySemesterOfferingAcademicTermIdAndStatus(source.getId(),AcademicStatus.ACTIVE);SemesterRolloverJob job=new SemesterRolloverJob();job.setCollege(source.getCollege());job.setSourceTerm(source);job.setTargetTerm(target);job.setStatus(SemesterRolloverStatus.RUNNING);job.setTotalStudents(sourceRows.size());job.setRequestedBy(users.findById(SecurityUtils.getCurrentUserId()).orElse(null));job.setStartedAt(LocalDateTime.now(clock));job=jobs.save(job);Set<Long> held=request.holdStudentIds()==null?Set.of():request.holdStudentIds();Map<Long,Long> sectionMap=request.targetSectionBySourceSection()==null?Map.of():request.targetSectionBySourceSection();int promoted=0,heldCount=0,graduated=0;for(StudentSectionEnrollment current:sourceRows){SemesterRolloverItem item=new SemesterRolloverItem();item.setJob(job);item.setStudent(current.getStudent());item.setSourceEnrollment(current);if(held.contains(current.getStudent().getId())){complete(current,EnrollmentCompletionStatus.HELD);item.setDecision(SemesterRolloverDecision.HOLD);item.setMessage("Held by Principal during rollover");heldCount++;items.save(item);continue;}CurriculumSemester currentSemester=current.getSemesterOffering().getCurriculumSemester();Optional<CurriculumSemester> next=semesters.findByDepartmentIdAndSemesterNumber(currentSemester.getDepartment().getId(),currentSemester.getSemesterNumber()+1).filter(CurriculumSemester::isActive);if(next.isEmpty()){complete(current,EnrollmentCompletionStatus.GRADUATED);item.setDecision(SemesterRolloverDecision.GRADUATE);item.setMessage("Completed final configured semester");graduated++;items.save(item);continue;}SemesterOffering targetOffering=offerings.findByAcademicTermIdAndCurriculumSemesterId(target.getId(),next.get().getId()).orElseThrow(()->new BadRequestException("Target offering missing for "+next.get().getName()+" in "+next.get().getDepartment().getName()));Section targetSection=targetSection(current,targetOffering,source,target,sectionMap);if(enrollments.countBySectionIdAndStatus(targetSection.getId(),AcademicStatus.ACTIVE)>=targetSection.getCapacity())throw new BadRequestException("Target division "+targetSection.getName()+" has reached capacity");if(enrollments.existsByStudentIdAndSemesterOfferingId(current.getStudent().getId(),targetOffering.getId()))throw new DuplicateResourceException(current.getStudent().getFullName()+" is already enrolled in the target semester");complete(current,EnrollmentCompletionStatus.PROMOTED);enrollments.flush();StudentSectionEnrollment created=new StudentSectionEnrollment();created.setStudent(current.getStudent());created.setSection(targetSection);created.setAcademicClass(targetSection.getAcademicClass());created.setSemesterOffering(targetOffering);created.setAcademicYear(target.getAcademicYear().getName());created.setRollNumber(current.getRollNumber());created=enrollments.save(created);item.setTargetEnrollment(created);item.setDecision(SemesterRolloverDecision.PROMOTE);item.setMessage("Promoted to "+next.get().getName());items.save(item);promoted++;}job.setPromotedStudents(promoted);job.setHeldStudents(heldCount);job.setGraduatedStudents(graduated);job.setStatus(SemesterRolloverStatus.COMPLETED);job.setCompletedAt(LocalDateTime.now(clock));activateYearForTransition(target.getAcademicYear());activateTermInternal(target);audit.log(AuditModule.ACADEMIC,AuditAction.SYSTEM_EVENT,"SemesterRollover",job.getId(),"Semester rollover completed: "+promoted+" promoted, "+heldCount+" held, "+graduated+" graduated");return new RolloverResult(job.getId(),job.getStatus(),sourceRows.size(),promoted,heldCount,graduated);}

    private void activateYearForTransition(AcademicYear target){years.findByCollegeIdAndStatus(collegeId(),AcademicYearStatus.ACTIVE).filter(x->!x.getId().equals(target.getId())).ifPresent(x->x.setStatus(AcademicYearStatus.CLOSED));target.setStatus(AcademicYearStatus.ACTIVE);}
    private void activateTermInternal(AcademicTerm target){LocalDate today=LocalDate.now(clock);terms.findByCollegeIdAndStatus(collegeId(),AcademicTermStatus.ACTIVE).filter(x->!x.getId().equals(target.getId())).ifPresent(x->{AcademicTermStatus status=x.getEndDate().isBefore(today)?AcademicTermStatus.CLOSED:AcademicTermStatus.PLANNED;x.setStatus(status);timetables.findBySemesterOfferingAcademicTermIdAndStatusNot(x.getId(),WeeklyTimetable.Status.ARCHIVED).forEach(table->table.setStatus(WeeklyTimetable.Status.ARCHIVED));SemesterOfferingStatus offeringStatus=status==AcademicTermStatus.CLOSED?SemesterOfferingStatus.CLOSED:SemesterOfferingStatus.PLANNED;offerings.findByAcademicTermIdOrderByDepartmentIdAscCurriculumSemesterSemesterNumberAsc(x.getId()).forEach(o->o.setStatus(offeringStatus));});target.setStatus(AcademicTermStatus.ACTIVE);for(SemesterOffering offering:offerings.findByAcademicTermIdOrderByDepartmentIdAscCurriculumSemesterSemesterNumberAsc(target.getId()))offering.setStatus(SemesterOfferingStatus.ACTIVE);}
    private Section targetSection(StudentSectionEnrollment current,SemesterOffering offering,AcademicTerm source,AcademicTerm target,Map<Long,Long> map){if(source.getAcademicYear().getId().equals(target.getAcademicYear().getId()))return current.getSection();Long id=map.get(current.getSection().getId());Section section=id==null?autoTargetSection(current,offering,target).orElseThrow(()->new BadRequestException("Create or select a target division for "+current.getSection().getName())):sections.findById(id).orElseThrow(()->new ResourceNotFoundException("Target division not found"));if(!section.getCollege().getId().equals(collegeId())||!section.getDepartment().getId().equals(offering.getDepartment().getId())||section.getAcademicClass().getYearName()!=offering.getCurriculumSemester().getYearName()||!normalizeYear(section.getAcademicYear()).equals(normalizeYear(target.getAcademicYear().getName())))throw new BadRequestException("Target division does not match the target academic year and semester");return section;}
    private Optional<Section> autoTargetSection(StudentSectionEnrollment current,SemesterOffering offering,AcademicTerm target){return sections.findRolloverTarget(collegeId(),offering.getDepartment().getId(),target.getAcademicYear().getName(),offering.getCurriculumSemester().getYearName(),current.getSection().getCode(),SectionStatus.ACTIVE);}
    private void complete(StudentSectionEnrollment row,EnrollmentCompletionStatus status){row.setStatus(AcademicStatus.INACTIVE);row.setCompletionStatus(status);row.setCompletedAt(LocalDateTime.now(clock));}
    private RolloverStudentPreview previewRow(StudentSectionEnrollment e,Integer target,String decision,String reason){return new RolloverStudentPreview(e.getId(),e.getStudent().getId(),e.getStudent().getFullName(),e.getSection().getName(),e.getSemesterOffering().getCurriculumSemester().getSemesterNumber(),target,decision,reason);}
    private void validateTransition(AcademicTerm source,AcademicTerm target){if(source.getId().equals(target.getId())||!target.getStartDate().isAfter(source.getStartDate()))throw new BadRequestException("Target term must be later than source term");if(source.getTermType()==target.getTermType())throw new BadRequestException("Semester rollover must alternate odd and even terms");}
    private boolean within(LocalDate date,AcademicTerm term){return !date.isBefore(term.getStartDate())&&!date.isAfter(term.getEndDate());}
    private void createOfferings(AcademicYear year){List<AcademicTerm> yearTerms=terms.findByAcademicYearIdOrderByStartDate(year.getId());Map<AcademicTermType,AcademicTerm> byType=new EnumMap<>(AcademicTermType.class);yearTerms.forEach(x->byType.put(x.getTermType(),x));for(CurriculumSemester semester:semesters.findByCollegeIdAndActiveTrueOrderByDepartmentIdAscSemesterNumberAsc(year.getCollege().getId())){AcademicTerm term=byType.get(semester.getTermType());if(term==null||offerings.findByAcademicTermIdAndCurriculumSemesterId(term.getId(),semester.getId()).isPresent())continue;SemesterOffering offering=new SemesterOffering();offering.setCollege(year.getCollege());offering.setDepartment(semester.getDepartment());offering.setAcademicYear(year);offering.setAcademicTerm(term);offering.setCurriculumSemester(semester);offering.setStatus(term.getStatus()==AcademicTermStatus.ACTIVE?SemesterOfferingStatus.ACTIVE:term.getStatus()==AcademicTermStatus.CLOSED?SemesterOfferingStatus.CLOSED:SemesterOfferingStatus.PLANNED);offerings.save(offering);}}
    private AcademicTerm createTerm(AcademicYear year,AcademicTermType type,String name,TermDates dates){AcademicTerm term=new AcademicTerm();term.setCollege(year.getCollege());term.setAcademicYear(year);term.setTermType(type);term.setName(name);term.setStartDate(dates.startDate());term.setEndDate(dates.endDate());term.setStatus(AcademicTermStatus.PLANNED);return terms.save(term);}
    private void validateYearDates(LocalDate start,LocalDate end){if(start.isAfter(end))throw new BadRequestException("Academic year start date must be before its end date");if(ChronoUnit.DAYS.between(start,end)<300||ChronoUnit.DAYS.between(start,end)>430)throw new BadRequestException("Academic year must be between 300 and 430 days");}
    private void validateTermDates(LocalDate start,LocalDate end,TermDates odd,TermDates even){if(odd.startDate().isBefore(start)||even.endDate().isAfter(end)||odd.startDate().isAfter(odd.endDate())||even.startDate().isAfter(even.endDate()))throw new BadRequestException("Semester dates must be valid and inside the academic year");if(!odd.endDate().isBefore(even.startDate()))throw new BadRequestException("Odd semester must end before the even semester starts");}
    private boolean overlaps(LocalDate a1,LocalDate a2,LocalDate b1,LocalDate b2){return !a2.isBefore(b1)&&!b2.isBefore(a1);}
    private CourseYearName yearName(int semester){return CourseYearName.values()[(semester-1)/2];}
    private String normalizeYear(String value){return value.trim().replace('/','-');}
    private Long collegeId(){Long id=SecurityUtils.requireCurrentUser().getCollegeId();if(id==null)throw new org.springframework.security.access.AccessDeniedException("College access is required");return id;}
    private AcademicYear year(Long id){return years.findByIdAndCollegeId(id,collegeId()).orElseThrow(()->new ResourceNotFoundException("Academic year not found"));}
    private AcademicTerm term(Long id){return terms.findByIdAndCollegeId(id,collegeId()).orElseThrow(()->new ResourceNotFoundException("Academic term not found"));}
    private Department department(Long id){Department row=departments.findById(id).orElseThrow(()->new ResourceNotFoundException("Department not found"));if(!row.getCollege().getId().equals(collegeId()))throw new org.springframework.security.access.AccessDeniedException("Department is outside your college");return row;}
    private AcademicYearView yearView(AcademicYear y){return new AcademicYearView(y.getId(),y.getName(),y.getStartDate(),y.getEndDate(),y.getStatus(),terms.findByAcademicYearIdOrderByStartDate(y.getId()).stream().map(this::termView).toList());}
    private AcademicTermView termView(AcademicTerm t){return new AcademicTermView(t.getId(),t.getAcademicYear().getId(),t.getName(),t.getTermType(),t.getStartDate(),t.getEndDate(),t.getStatus());}
    private SemesterView semesterView(CurriculumSemester s){return new SemesterView(s.getId(),s.getDepartment().getId(),s.getDepartment().getName(),s.getSemesterNumber(),s.getYearName(),s.getTermType(),s.getName(),s.isActive());}
    private OfferingView offeringView(SemesterOffering o){return new OfferingView(o.getId(),o.getAcademicYear().getId(),o.getAcademicYear().getName(),o.getAcademicTerm().getId(),o.getAcademicTerm().getTermType(),o.getDepartment().getId(),o.getDepartment().getName(),o.getCurriculumSemester().getId(),o.getCurriculumSemester().getSemesterNumber(),o.getCurriculumSemester().getYearName(),o.getStatus());}
}

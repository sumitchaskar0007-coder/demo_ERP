package com.jadhavr.erp.timetable.service;

import com.jadhavr.erp.academic.entity.*;
import com.jadhavr.erp.academic.enums.*;
import com.jadhavr.erp.academic.repository.*;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.*;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.timetable.dto.WeeklyTimetableDtos.*;
import com.jadhavr.erp.timetable.entity.*;
import com.jadhavr.erp.timetable.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @Transactional
public class WeeklyTimetableService {
    private static final List<DayOfWeek> DAYS=List.of(DayOfWeek.MONDAY,DayOfWeek.TUESDAY,DayOfWeek.WEDNESDAY,DayOfWeek.THURSDAY,DayOfWeek.FRIDAY,DayOfWeek.SATURDAY);
    private final WeeklyTimetableRepository tables; private final WeeklyPeriodRepository periods; private final WeeklyTimetableEntryRepository entries;
    private final SectionRepository sections; private final SubjectRepository subjects; private final StaffProfileRepository staff; private final SubjectTeacherAssignmentRepository subjectTeacherAssignments;
    public WeeklyTimetableService(WeeklyTimetableRepository t,WeeklyPeriodRepository p,WeeklyTimetableEntryRepository e,SectionRepository s,SubjectRepository u,StaffProfileRepository f,SubjectTeacherAssignmentRepository a){tables=t;periods=p;entries=e;sections=s;subjects=u;staff=f;subjectTeacherAssignments=a;}

    @Transactional(readOnly=true) public List<DivisionOption> divisions(){requireViewer();return sections.findAll().stream().filter(this::visible).filter(s->s.getStatus()==SectionStatus.ACTIVE).map(s->new DivisionOption(s.getId(),s.getDepartment().getName(),s.getAcademicClass().getName(),s.getName(),s.getClassTeacher()==null?"Not assigned":s.getClassTeacher().getFullName(),s.getAcademicYear(),editable(s))).toList();}
    public TimetableResponse getOrCreate(Long sectionId){Section s=section(sectionId);WeeklyTimetable t=tables.findBySectionId(sectionId).orElseGet(()->{if(!editable(s))throw new ResourceNotFoundException("Timetable has not been created yet");return create(s);});return map(t);}
    public EntryResponse save(Long id,String dayValue,Long periodId,SaveEntryRequest r){WeeklyTimetable t=table(id);requireEditor(t.getSection());DayOfWeek day=day(dayValue);WeeklyPeriod p=period(t,periodId);if(p.getKind()!=WeeklyPeriod.Kind.TEACHING)throw new BadRequestException("Break cells cannot contain lectures");
        Subject subject=subjects.findById(r.subjectId()).orElseThrow(()->new ResourceNotFoundException("Subject not found"));if(!subject.getAcademicClass().getId().equals(t.getSection().getAcademicClass().getId())||subject.getStatus()!=SubjectStatus.ACTIVE)throw new BadRequestException("Subject does not belong to this course year");
        StaffProfile teacher=staff.findById(r.teacherId()).orElseThrow(()->new ResourceNotFoundException("Teacher not found"));if(teacher.getStatus()!=StaffStatus.ACTIVE||teacher.getDepartment()==null||!teacher.getDepartment().getId().equals(t.getSection().getDepartment().getId()))throw new BadRequestException("Teacher does not belong to this department");if(!subjectTeacherAssignments.existsBySubjectIdAndTeacherIdAndStatus(subject.getId(),teacher.getId(),AcademicStatus.ACTIVE))throw new BadRequestException("Selected teacher is not assigned to this subject");
        WeeklyTimetableEntry e=entries.findByTimetableIdAndDayOfWeekAndPeriodId(id,day,periodId).orElseGet(WeeklyTimetableEntry::new);Long exclude=e.getId();if(entries.teacherConflicts(t.getCollege().getId(),day,teacher.getId(),p.getStartTime(),p.getEndTime(),exclude)>0)throw new BadRequestException("Teacher already has a lecture at this time");
        e.setTimetable(t);e.setDayOfWeek(day);e.setPeriod(p);e.setSubject(subject);e.setTeacher(teacher);e.setRoom(trim(r.room()));e.setRemarks(trim(r.remarks()));try{e.setLectureType(WeeklyTimetableEntry.LectureType.valueOf(r.lectureType().toUpperCase(Locale.ROOT)));}catch(Exception x){throw new BadRequestException("Invalid lecture type");}return entry(entries.save(e));}
    public void delete(Long id,String dayValue,Long periodId){WeeklyTimetable t=table(id);requireEditor(t.getSection());entries.findByTimetableIdAndDayOfWeekAndPeriodId(id,day(dayValue),periodId).ifPresent(entries::delete);}
    public TimetableResponse move(Long id,MoveEntryRequest r){WeeklyTimetable t=table(id);requireEditor(t.getSection());DayOfWeek from=day(r.fromDay()),to=day(r.toDay());WeeklyTimetableEntry source=entries.findByTimetableIdAndDayOfWeekAndPeriodId(id,from,r.fromPeriodId()).orElseThrow(()->new ResourceNotFoundException("Lecture not found"));WeeklyPeriod target=period(t,r.toPeriodId());if(target.getKind()!=WeeklyPeriod.Kind.TEACHING)throw new BadRequestException("Cannot move a lecture into a break");if(entries.findByTimetableIdAndDayOfWeekAndPeriodId(id,to,target.getId()).isPresent())throw new BadRequestException("Target cell already contains a lecture");if(entries.teacherConflicts(t.getCollege().getId(),to,source.getTeacher().getId(),target.getStartTime(),target.getEndTime(),source.getId())>0)throw new BadRequestException("Teacher already has a lecture at this time");source.setDayOfWeek(to);source.setPeriod(target);entries.save(source);return map(t);}
    public TimetableResponse updatePeriods(Long id,UpdatePeriodsRequest r){
        WeeklyTimetable t=table(id);requireEditor(t.getSection());
        List<PeriodItem> requested=r.periods(); validatePeriods(requested);
        Map<Long,WeeklyPeriod> owned=new HashMap<>();periods.findByTimetableIdOrderByPosition(id).forEach(p->owned.put(p.getId(),p));
        Set<Long> retained=new HashSet<>();
        for(PeriodItem item:requested){if(item.id()!=null){if(!owned.containsKey(item.id())||!retained.add(item.id()))throw new BadRequestException("Invalid or repeated period");}}
        // Move existing positions out of the way first so a reordered list never violates its unique constraint.
        owned.values().forEach(p->p.setPosition(p.getPosition()+1000)); periods.saveAll(owned.values()); periods.flush();
        for(WeeklyPeriod removed:owned.values())if(!retained.contains(removed.getId())){entries.deleteAll(entries.findByPeriodId(removed.getId()));periods.delete(removed);}
        for(int index=0;index<requested.size();index++){
            PeriodItem item=requested.get(index); WeeklyPeriod p=item.id()==null?new WeeklyPeriod():owned.get(item.id());
            if(item.id()==null)p.setTimetable(t);
            if(p.getId()!=null&&p.getKind()==WeeklyPeriod.Kind.TEACHING&&item.kind()!=WeeklyPeriod.Kind.TEACHING)entries.deleteAll(entries.findByPeriodId(p.getId()));
            p.setPosition(index+1);p.setLabel(item.label().trim());p.setStartTime(item.startTime());p.setEndTime(item.endTime());p.setKind(item.kind());periods.save(p);
        }
        periods.flush(); return map(t);
    }
    private void validatePeriods(List<PeriodItem> items){
        if(items.isEmpty())throw new BadRequestException("Add at least one period");
        LocalTime previousEnd=null;
        for(PeriodItem item:items){
            if(!item.endTime().isAfter(item.startTime()))throw new BadRequestException("Each period must end after it starts");
            if(previousEnd!=null&&item.startTime().isBefore(previousEnd))throw new BadRequestException("Period times cannot overlap and must be in order");
            previousEnd=item.endTime();
        }
    }

    private WeeklyTimetable create(Section s){requireEditor(s);WeeklyTimetable t=new WeeklyTimetable();t.setCollege(s.getCollege());t.setSection(s);t=tables.save(t);String[][] defaults={{"Period 1","08:30","09:20","TEACHING"},{"Period 2","09:20","10:10","TEACHING"},{"Short Break","10:10","10:25","SHORT_BREAK"},{"Period 3","10:25","11:15","TEACHING"},{"Period 4","11:15","12:05","TEACHING"},{"Lunch Break","12:05","12:45","LUNCH_BREAK"},{"Period 5","12:45","13:35","TEACHING"},{"Period 6","13:35","14:25","TEACHING"},{"Period 7","14:25","15:15","TEACHING"}};for(int i=0;i<defaults.length;i++){WeeklyPeriod p=new WeeklyPeriod();p.setTimetable(t);p.setPosition(i+1);p.setLabel(defaults[i][0]);p.setStartTime(LocalTime.parse(defaults[i][1]));p.setEndTime(LocalTime.parse(defaults[i][2]));p.setKind(WeeklyPeriod.Kind.valueOf(defaults[i][3]));periods.save(p);}return t;}
    private TimetableResponse map(WeeklyTimetable t){Section s=t.getSection();List<Option> subjectOptions=subjects.findAll().stream().filter(x->x.getAcademicClass().getId().equals(s.getAcademicClass().getId())&&x.getStatus()==SubjectStatus.ACTIVE).map(x->new Option(x.getId(),x.getCode()+" - "+x.getName())).toList();Set<StaffType> teachingTypes=EnumSet.of(StaffType.HOD,StaffType.TEACHER,StaffType.CLASS_TEACHER,StaffType.SUBJECT_TEACHER);List<StaffProfile> departmentTeachers=staff.findByCollegeId(s.getCollege().getId()).stream().filter(x->x.getDepartment()!=null&&x.getDepartment().getId().equals(s.getDepartment().getId())&&x.getStatus()==StaffStatus.ACTIVE&&teachingTypes.contains(x.getStaffType())).toList();List<Option> teacherOptions=departmentTeachers.stream().map(x->new Option(x.getId(),x.getFullName())).toList();List<SubjectTeacherOption> subjectTeachers=subjectOptions.stream().map(option->new SubjectTeacherOption(option.id(), departmentTeachers.stream().filter(teacher->subjectTeacherAssignments.existsBySubjectIdAndTeacherIdAndStatus(option.id(),teacher.getId(),AcademicStatus.ACTIVE)).map(teacher->new Option(teacher.getId(),teacher.getFullName())).toList())).toList();List<EntryResponse> es=entries.findByTimetableId(t.getId()).stream().map(this::entry).toList();List<String> rooms=es.stream().map(EntryResponse::room).filter(Objects::nonNull).distinct().toList();return new TimetableResponse(t.getId(),s.getId(),s.getDepartment().getName(),s.getAcademicClass().getName(),s.getName(),s.getClassTeacher()==null?"Not assigned":s.getClassTeacher().getFullName(),s.getAcademicYear(),t.getStatus().name(),editable(s),periods.findByTimetableIdOrderByPosition(t.getId()).stream().map(p->new PeriodResponse(p.getId(),p.getPosition(),p.getLabel(),p.getStartTime(),p.getEndTime(),p.getKind().name())).toList(),es,subjectOptions,teacherOptions,subjectTeachers,rooms);}
    private EntryResponse entry(WeeklyTimetableEntry e){return new EntryResponse(e.getId(),e.getDayOfWeek().name(),e.getPeriod().getId(),e.getSubject().getId(),e.getSubject().getName(),e.getTeacher().getId(),e.getTeacher().getFullName(),e.getRoom(),e.getLectureType().name(),e.getRemarks());}
    private WeeklyTimetable table(Long id){WeeklyTimetable t=tables.findById(id).orElseThrow(()->new ResourceNotFoundException("Weekly timetable not found"));if(!visible(t.getSection()))throw new AccessDeniedException("Timetable is outside your scope");return t;}
    private Section section(Long id){Section s=sections.findById(id).orElseThrow(()->new ResourceNotFoundException("Division not found"));if(!visible(s))throw new AccessDeniedException("Division is outside your scope");return s;}
    private WeeklyPeriod period(WeeklyTimetable t,Long id){WeeklyPeriod p=periods.findById(id).orElseThrow(()->new ResourceNotFoundException("Period not found"));if(!p.getTimetable().getId().equals(t.getId()))throw new BadRequestException("Period does not belong to this timetable");return p;}
    private void requireViewer(){if(!(SecurityUtils.isSuperAdmin()||SecurityUtils.hasRole("PRINCIPAL")||SecurityUtils.hasRole("HOD")||SecurityUtils.hasRole("CLASS_TEACHER")))throw new AccessDeniedException("Weekly timetable access denied");}
    private void requireEditor(Section s){if(!editable(s))throw new AccessDeniedException("You have read-only timetable access");}
    private boolean visible(Section s){requireViewer();if(SecurityUtils.isSuperAdmin())return true;var u=SecurityUtils.requireCurrentUser();if(!Objects.equals(u.getCollegeId(),s.getCollege().getId()))return false;Optional<StaffProfile> profile=staff.findByUserId(u.getId());if(SecurityUtils.hasRole("HOD"))return profile.map(x->x.getDepartment()!=null&&x.getDepartment().getId().equals(s.getDepartment().getId())).orElse(false);if(SecurityUtils.hasRole("CLASS_TEACHER"))return profile.map(x->s.getClassTeacher()!=null&&s.getClassTeacher().getId().equals(x.getId())).orElse(false);return SecurityUtils.hasRole("PRINCIPAL");}
    private boolean editable(Section s){if(SecurityUtils.isSuperAdmin()||SecurityUtils.hasRole("PRINCIPAL"))return true;if(!SecurityUtils.hasRole("CLASS_TEACHER"))return false;return staff.findByUserId(SecurityUtils.getCurrentUserId()).map(x->s.getClassTeacher()!=null&&s.getClassTeacher().getId().equals(x.getId())).orElse(false);}
    private DayOfWeek day(String value){try{DayOfWeek d=DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT));if(!DAYS.contains(d))throw new Exception();return d;}catch(Exception e){throw new BadRequestException("Invalid timetable day");}}
    private String trim(String value){return value==null||value.isBlank()?null:value.trim();}
}

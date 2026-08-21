package com.collegeerp.erp.timetable.repository;
import com.collegeerp.erp.timetable.entity.TimetableModels.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TimetableRepository extends JpaRepository<Timetable,Long>{
 Optional<Timetable> findByIdAndCollegeId(Long id,Long collegeId);
 List<Timetable> findByCollegeIdOrderByWeekStartDesc(Long collegeId);
}

package com.jadhavr.erp.timetable.repository;
import com.jadhavr.erp.timetable.entity.WeeklyPeriod; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface WeeklyPeriodRepository extends JpaRepository<WeeklyPeriod,Long>{List<WeeklyPeriod> findByTimetableIdOrderByPosition(Long timetableId);}

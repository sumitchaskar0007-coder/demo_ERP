package com.jadhavr.erp.timetable.dto;

import java.time.LocalTime;
import java.util.List;

public final class TeacherTimetableDtos {
    private TeacherTimetableDtos() {}

    public record PeriodResponse(String key, int position, String label, LocalTime startTime,
                                 LocalTime endTime, String kind) {}

    public record LectureResponse(Long id, String dayOfWeek, String periodKey, String periodLabel,
                                  LocalTime startTime, LocalTime endTime, Long subjectId,
                                  String subject, String department, String year, String division,
                                  String lectureType, String remarks) {}

    public record TimetableResponse(String teacherName, String employeeId, long totalWeeklyLectures,
                                    long todayLectureCount, String currentDay,
                                    List<PeriodResponse> periods, List<LectureResponse> lectures) {}

    public record DayResponse(String day, List<PeriodResponse> periods,
                              List<LectureResponse> lectures) {}

    public record NextLectureResponse(LectureResponse lecture, Long startsInMinutes) {}
}

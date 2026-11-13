package com.yongyongthon.be.course.service;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.response.RequestValueInvalidException;
import com.yongyongthon.be.course.data.dto.CalendarDateResponseDto;
import com.yongyongthon.be.course.data.dto.CalendarMonthResponseDto;
import com.yongyongthon.be.course.data.entity.Course;
import com.yongyongthon.be.course.data.repository.CourseRepository;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.service.EventCatalog;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import com.yongyongthon.be.itinerary.data.repository.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 행사 캘린더 (지시서 7장 #15). month 또는 date 중 하나만 받는다.
 * events는 관심 행사 중 기간이 겹치는 것, courses는 저장한 코스 중 일정 날짜가 범위에 드는 것.
 */
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CourseRepository courseRepository;
    private final ItineraryRepository itineraryRepository;
    private final EventCatalog eventCatalog;
    private final InterestEventProvider interestEventProvider;

    @Transactional(readOnly = true)
    public GlobalResponse<?> getCalendar(String month, String date) {
        if ((month == null) == (date == null)) {
            throw new RequestValueInvalidException();
        }
        if (month != null) {
            return GlobalResponse.ok(byMonth(parseMonth(month)));
        }
        return GlobalResponse.ok(byDate(parseDate(date)));
    }

    private CalendarMonthResponseDto byMonth(YearMonth ym) {
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        List<CalendarMonthResponseDto.MonthEvent> events = likedEvents(from, to).stream()
                .map(e -> new CalendarMonthResponseDto.MonthEvent(
                        e.eventId(), e.name(), e.startDate().toString(), e.endDate().toString()))
                .toList();
        List<CalendarMonthResponseDto.MonthCourse> courses = savedCourses(from, to).stream()
                .map(cc -> new CalendarMonthResponseDto.MonthCourse(
                        cc.itinerary().getItineraryId(), cc.course().getTitle(), cc.itinerary().getDate().toString()))
                .toList();
        return new CalendarMonthResponseDto(ym.toString(), events, courses);
    }

    private CalendarDateResponseDto byDate(LocalDate day) {
        List<CalendarDateResponseDto.DateEvent> events = likedEvents(day, day).stream()
                .map(e -> new CalendarDateResponseDto.DateEvent(
                        e.eventId(), e.name(), e.category(), e.startDate().toString(), e.endDate().toString()))
                .toList();
        List<CalendarDateResponseDto.DateCourse> courses = savedCourses(day, day).stream()
                .map(cc -> new CalendarDateResponseDto.DateCourse(
                        cc.itinerary().getItineraryId(), cc.course().getTitle(), cc.itinerary().getSlots().size()))
                .toList();
        return new CalendarDateResponseDto(day.toString(), events.size(), courses.size(), events, courses);
    }

    private List<EventInfo> likedEvents(LocalDate from, LocalDate to) {
        return eventCatalog.findByIdsOverlapping(interestEventProvider.likedEventIds(), from, to);
    }

    private record CourseWithItinerary(Course course, Itinerary itinerary) {
    }

    /** 사용자 구분이 없어서 저장된 코스 전체에서 고른다 */
    private List<CourseWithItinerary> savedCourses(LocalDate from, LocalDate to) {
        List<Course> all = courseRepository.findAll();
        Map<String, Itinerary> itineraries = itineraryRepository
                .findAllById(all.stream().map(Course::getItineraryId).toList()).stream()
                .collect(Collectors.toMap(Itinerary::getItineraryId, Function.identity()));
        List<CourseWithItinerary> result = new ArrayList<>();
        for (Course c : all) {
            Itinerary it = itineraries.get(c.getItineraryId());
            if (it != null && !it.getDate().isBefore(from) && !it.getDate().isAfter(to)) {
                result.add(new CourseWithItinerary(c, it));
            }
        }
        result.sort(Comparator.comparing((CourseWithItinerary cc) -> cc.itinerary().getDate())
                .thenComparing(cc -> cc.course().getSavedAt()));
        return result;
    }

    private static YearMonth parseMonth(String text) {
        if (!text.matches("\\d{4}-\\d{2}")) {
            throw new RequestValueInvalidException();
        }
        try {
            return YearMonth.parse(text);
        } catch (DateTimeParseException e) {
            throw new RequestValueInvalidException();
        }
    }

    private static LocalDate parseDate(String text) {
        if (!text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new RequestValueInvalidException();
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new RequestValueInvalidException();
        }
    }
}

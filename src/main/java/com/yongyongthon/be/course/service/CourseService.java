package com.yongyongthon.be.course.service;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.course.data.dto.CourseDeleteResponseDto;
import com.yongyongthon.be.course.data.dto.CourseListResponseDto;
import com.yongyongthon.be.course.data.dto.CourseSaveRequestDto;
import com.yongyongthon.be.course.data.dto.CourseSaveResponseDto;
import com.yongyongthon.be.course.data.entity.Course;
import com.yongyongthon.be.course.data.exception.CourseNotFoundException;
import com.yongyongthon.be.course.data.repository.CourseRepository;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import com.yongyongthon.be.itinerary.data.entity.ItinerarySlot;
import com.yongyongthon.be.itinerary.data.enums.SlotType;
import com.yongyongthon.be.itinerary.data.exception.InvalidItineraryRequestException;
import com.yongyongthon.be.itinerary.data.exception.ItineraryNotFoundException;
import com.yongyongthon.be.itinerary.data.repository.ItineraryRepository;
import com.yongyongthon.be.itinerary.service.SlotFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 내 코스 저장, 목록, 삭제 (지시서 7장 #12, #13, #14).
 * 사용자 구분이 없어서 서버의 모든 사용자가 같은 "내 코스"를 본다 (지시서 3-2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    public static final DateTimeFormatter SAVED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String COURSE_ID_PREFIX = "crs_";
    private static final int TITLE_MAX = 40;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    /** 가운데점(U+00B7). 지시서 7장 #13 summary 형식 그대로 */
    private static final String DOT = " \u00B7 ";

    private final CourseRepository courseRepository;
    private final ItineraryRepository itineraryRepository;

    /** #12 같은 일정은 한 번만 저장. 다시 요청하면 기존 코스를 돌려주고 제목이 오면 제목만 갱신 */
    @Transactional
    public GlobalResponse<CourseSaveResponseDto> saveCourse(CourseSaveRequestDto req) {
        if (req == null || req.itineraryId() == null || req.itineraryId().isBlank()) {
            throw new InvalidItineraryRequestException();
        }
        String title = req.title() == null || req.title().isBlank() ? null : req.title().trim();
        if (title != null && title.length() > TITLE_MAX) {
            throw new InvalidItineraryRequestException();
        }
        Itinerary it = itineraryRepository.findById(req.itineraryId())
                .orElseThrow(ItineraryNotFoundException::new);

        Course course = courseRepository.findByItineraryId(it.getItineraryId())
                .map(existing -> {
                    if (title != null) {
                        existing.setTitle(title);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Course created = courseRepository.save(Course.builder()
                            .itineraryId(it.getItineraryId())
                            .title(title != null ? title : it.getTitle())
                            .savedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                            .build());
                    log.info("[saveCourse] 코스 저장: {} {}", created.courseId(), it.getItineraryId());
                    return created;
                });

        return GlobalResponse.ok(new CourseSaveResponseDto(
                course.courseId(), course.getItineraryId(), course.getTitle(), true,
                course.getSavedAt().format(SAVED_AT)));
    }

    /** #13 최근 저장 순. 없으면 totalCount 0, 빈 목록 (오류 아님) */
    @Transactional(readOnly = true)
    public GlobalResponse<CourseListResponseDto> getCourses(Integer page, Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 0 || s < 1 || s > MAX_SIZE) {
            throw new InvalidItineraryRequestException();
        }
        Page<Course> result = courseRepository.findAll(
                PageRequest.of(p, s, Sort.by(Sort.Order.desc("savedAt"), Sort.Order.desc("id"))));

        List<CourseListResponseDto.CourseItem> items = result.getContent().stream()
                .map(c -> itineraryRepository.findById(c.getItineraryId())
                        .map(it -> toItem(c, it))
                        .orElse(null))
                .filter(i -> i != null)
                .toList();
        return GlobalResponse.ok(new CourseListResponseDto(result.getTotalElements(), p, s, items));
    }

    /** #14 코스(저장 기록)만 지운다. 일정과 공유 링크는 남는다 */
    @Transactional
    public GlobalResponse<CourseDeleteResponseDto> deleteCourse(String courseId) {
        Course course = courseRepository.findById(parseCourseId(courseId))
                .orElseThrow(CourseNotFoundException::new);
        courseRepository.delete(course);
        log.info("[deleteCourse] 코스 삭제: {}", courseId);
        return GlobalResponse.ok(new CourseDeleteResponseDto(courseId, true));
    }

    private CourseListResponseDto.CourseItem toItem(Course c, Itinerary it) {
        List<ItinerarySlot> slots = it.orderedSlots();
        String baseName = slots.stream()
                .filter(sl -> SlotType.EVENT.equals(sl.getType()))
                .map(ItinerarySlot::getTitle)
                .findFirst()
                .orElse(it.getTitle());
        String summary = baseName + DOT + "주변 먹거리" + DOT + "볼거리" + DOT + "귀가까지 " + slots.size() + "개 일정";
        String mealSummary = slots.stream()
                .filter(sl -> SlotType.MEAL.equals(sl.getType()))
                .filter(sl -> !SlotFactory.isEmptyMeal(sl))
                .map(ItinerarySlot::getTitle)
                .findFirst()
                .orElse(null);
        return new CourseListResponseDto.CourseItem(
                c.courseId(), it.getItineraryId(), c.getTitle(), it.getDate().toString(),
                it.getCompanion(), it.getTransport(), slots.size(), summary, mealSummary,
                it.getCoverImageUrl(), c.getSavedAt().format(SAVED_AT));
    }

    private static Long parseCourseId(String courseId) {
        if (courseId == null || !courseId.matches(COURSE_ID_PREFIX + "\\d{1,18}")) {
            throw new CourseNotFoundException();
        }
        return Long.parseLong(courseId.substring(COURSE_ID_PREFIX.length()));
    }
}

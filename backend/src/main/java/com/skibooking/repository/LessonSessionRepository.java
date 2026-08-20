package com.skibooking.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skibooking.entity.LessonSession;
import com.skibooking.entity.enums.LessonSessionStatus;

import jakarta.persistence.LockModeType;

public interface LessonSessionRepository extends JpaRepository<LessonSession, Long> {

    boolean existsByProductIdAndSessionDateAndStartTime(
            Long productId,
            LocalDate sessionDate,
            LocalTime startTime);

    List<LessonSession> findByProductIdAndSessionDateAndStatusOrderByStartTimeAsc(
            Long productId,
            LocalDate sessionDate,
            LessonSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from LessonSession session where session.id = :id")
    Optional<LessonSession> findByIdForUpdate(@Param("id") Long id);
}

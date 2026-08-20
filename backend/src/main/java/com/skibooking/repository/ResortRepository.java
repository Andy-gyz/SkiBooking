package com.skibooking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skibooking.entity.Resort;
import com.skibooking.entity.enums.ResortStatus;

public interface ResortRepository extends JpaRepository<Resort, Long> {

    Optional<Resort> findByName(String name);

    List<Resort> findByStatusOrderByNameAsc(ResortStatus status);

    Optional<Resort> findByIdAndStatus(Long id, ResortStatus status);
}

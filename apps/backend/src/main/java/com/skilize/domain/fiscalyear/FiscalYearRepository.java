package com.skilize.domain.fiscalyear;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, Integer> {

    @Query("SELECT f FROM FiscalYear f WHERE f.active = true AND :today BETWEEN f.startDate AND f.endDate ORDER BY f.startDate DESC")
    Optional<FiscalYear> findCurrent(@Param("today") LocalDate today);
}

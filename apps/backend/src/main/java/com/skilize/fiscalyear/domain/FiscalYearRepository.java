package com.skilize.fiscalyear.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/** 年度リポジトリ。 */
public interface FiscalYearRepository extends JpaRepository<FiscalYear, Integer> {

    /**
     * 今日の日付が有効期間（startDate〜endDate）に含まれる年度を返す。
     * BETWEEN は両端を含む（startDate ≤ today ≤ endDate）。有効フラグ（active=true）も確認する。
     */
    @Query("SELECT f FROM FiscalYear f WHERE f.active = true AND :today BETWEEN f.startDate AND f.endDate ORDER BY f.startDate DESC")
    Optional<FiscalYear> findCurrent(@Param("today") LocalDate today);
}

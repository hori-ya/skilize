package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** セミナー分類リポジトリ。自由入力セミナーの分類（ADセミナーとは別系統）。 */
public interface SeminarCategoryRepository extends JpaRepository<SeminarCategory, Integer> {
    /** 有効なセミナー分類を表示順昇順で返す。 */
    List<SeminarCategory> findByActiveTrueOrderBySortOrderAsc();
}

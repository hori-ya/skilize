package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 資格リポジトリ。分類（category）を LEFT JOIN FETCH で取得する。
 * 資格は分類なし（category=null）の場合があるため LEFT JOIN を使用する。
 * マスタ管理画面向けは「分類の並順 → 資格の並順」でソートする。分類なしは末尾（NULLS LAST）。
 */
public interface QualificationRepository extends JpaRepository<Qualification, Integer> {

    /** 全資格をマスタ管理画面向けソート順（分類→並順）で取得する（有効・無効含む全件）。 */
    @Query("SELECT q FROM Qualification q LEFT JOIN FETCH q.category ORDER BY q.category.sortOrder ASC NULLS LAST, q.sortOrder ASC")
    List<Qualification> findAllWithCategory();

    /** 有効な資格のみを分類付きで取得する。棚卸入力画面の選択肢に使用（ソート順は変更なし）。 */
    @Query("SELECT q FROM Qualification q LEFT JOIN FETCH q.category WHERE q.active = true ORDER BY q.sortOrder ASC")
    List<Qualification> findAllActiveWithCategory();

    /** 有効フラグを指定して資格をマスタ管理画面向けソート順（分類→並順）で取得する。 */
    @Query("SELECT q FROM Qualification q LEFT JOIN FETCH q.category WHERE q.active = :active ORDER BY q.category.sortOrder ASC NULLS LAST, q.sortOrder ASC")
    List<Qualification> findAllWithCategoryByActive(@Param("active") boolean active);
}

package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * ADセミナーリポジトリ。分類（category）を LEFT JOIN FETCH で取得する。
 * ADセミナーは分類なし（category=null）の場合があるため LEFT JOIN を使用する。
 * マスタ管理画面向けは「分類の並順 → ADセミナーの並順」でソートする。分類なしは末尾（NULLS LAST）。
 */
public interface AdSeminarRepository extends JpaRepository<AdSeminar, Integer> {

    /** 全ADセミナーをマスタ管理画面向けソート順（分類→並順）で取得する（有効・無効含む全件）。 */
    @Query("SELECT a FROM AdSeminar a LEFT JOIN FETCH a.category ORDER BY a.category.sortOrder ASC NULLS LAST, a.sortOrder ASC")
    List<AdSeminar> findAllWithCategory();

    /** 有効なADセミナーのみを分類付きで取得する。棚卸入力画面の選択肢に使用（ソート順は変更なし）。 */
    @Query("SELECT a FROM AdSeminar a LEFT JOIN FETCH a.category WHERE a.active = true ORDER BY a.sortOrder ASC")
    List<AdSeminar> findAllActiveWithCategory();

    /** 有効フラグを指定してADセミナーをマスタ管理画面向けソート順（分類→並順）で取得する。 */
    @Query("SELECT a FROM AdSeminar a LEFT JOIN FETCH a.category WHERE a.active = :active ORDER BY a.category.sortOrder ASC NULLS LAST, a.sortOrder ASC")
    List<AdSeminar> findAllWithCategoryByActive(@Param("active") boolean active);
}

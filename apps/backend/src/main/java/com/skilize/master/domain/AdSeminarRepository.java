package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * ADセミナーリポジトリ。分類（category）を LEFT JOIN FETCH で取得する。
 * ADセミナーは分類なし（category=null）の場合があるため LEFT JOIN を使用する。
 */
public interface AdSeminarRepository extends JpaRepository<AdSeminar, Integer> {

    /** 全ADセミナーを分類付きで取得する（有効・無効含む全件）。 */
    @Query("SELECT a FROM AdSeminar a LEFT JOIN FETCH a.category ORDER BY a.sortOrder ASC")
    List<AdSeminar> findAllWithCategory();

    /** 有効なADセミナーのみを分類付きで取得する。棚卸入力画面の選択肢に使用。 */
    @Query("SELECT a FROM AdSeminar a LEFT JOIN FETCH a.category WHERE a.active = true ORDER BY a.sortOrder ASC")
    List<AdSeminar> findAllActiveWithCategory();

    /** 有効フラグを指定してADセミナーを分類付きで取得する。 */
    @Query("SELECT a FROM AdSeminar a LEFT JOIN FETCH a.category WHERE a.active = :active ORDER BY a.sortOrder ASC")
    List<AdSeminar> findAllWithCategoryByActive(@Param("active") boolean active);
}

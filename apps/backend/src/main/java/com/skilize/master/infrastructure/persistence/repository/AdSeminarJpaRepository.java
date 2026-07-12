/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナーマスタの Spring Data JPA リポジトリ。
 * 分類を LEFT JOIN FETCH で取得し、マスタ管理画面と棚卸入力画面向けの取得メソッドを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.AdSeminarRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.infrastructure.persistence.entity.AdSeminarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * ADセミナー Spring Data JPA リポジトリ。分類（category）を LEFT JOIN FETCH で取得する。
 * ADセミナーは分類なし（category=null）の場合があるため LEFT JOIN を使用する。
 * マスタ管理画面向けは「分類の並順 → ADセミナーの並順」でソートする。分類なしは末尾（NULLS LAST）。
 */
public interface AdSeminarJpaRepository extends JpaRepository<AdSeminarEntity, Integer> {

    /** 全ADセミナーをマスタ管理画面向けソート順（分類→並順）で取得する（有効・無効含む全件）。 */
    @Query("SELECT a FROM AdSeminarEntity a LEFT JOIN FETCH a.category ORDER BY a.category.sortOrder ASC NULLS LAST, a.sortOrder ASC")
    List<AdSeminarEntity> findAllWithCategory();

    /** 有効なADセミナーのみを分類付きで取得する。棚卸入力画面の選択肢に使用（ソート順は変更なし）。 */
    @Query("SELECT a FROM AdSeminarEntity a LEFT JOIN FETCH a.category WHERE a.active = true ORDER BY a.sortOrder ASC")
    List<AdSeminarEntity> findAllActiveWithCategory();

    /** 有効フラグを指定してADセミナーをマスタ管理画面向けソート順（分類→並順）で取得する。 */
    @Query("SELECT a FROM AdSeminarEntity a LEFT JOIN FETCH a.category WHERE a.active = :active ORDER BY a.category.sortOrder ASC NULLS LAST, a.sortOrder ASC")
    List<AdSeminarEntity> findAllWithCategoryByActive(@Param("active") boolean active);
}

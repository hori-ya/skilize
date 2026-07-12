/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格マスタの Spring Data JPA リポジトリ。
 * 分類を LEFT JOIN FETCH で取得し、マスタ管理画面と棚卸入力画面向けの取得メソッドを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.QualificationRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.infrastructure.persistence.entity.QualificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 資格 Spring Data JPA リポジトリ。分類（category）を LEFT JOIN FETCH で取得する。
 * 資格は分類なし（category=null）の場合があるため LEFT JOIN を使用する。
 * マスタ管理画面向けは「分類の並順 → 資格の並順」でソートする。分類なしは末尾（NULLS LAST）。
 */
public interface QualificationJpaRepository extends JpaRepository<QualificationEntity, Integer> {

    /** 全資格をマスタ管理画面向けソート順（分類→並順）で取得する（有効・無効含む全件）。 */
    @Query("SELECT q FROM QualificationEntity q LEFT JOIN FETCH q.category ORDER BY q.category.sortOrder ASC NULLS LAST, q.sortOrder ASC")
    List<QualificationEntity> findAllWithCategory();

    /** 有効な資格のみを分類付きで取得する。棚卸入力画面の選択肢に使用（ソート順は変更なし）。 */
    @Query("SELECT q FROM QualificationEntity q LEFT JOIN FETCH q.category WHERE q.active = true ORDER BY q.sortOrder ASC")
    List<QualificationEntity> findAllActiveWithCategory();

    /** 有効フラグを指定して資格をマスタ管理画面向けソート順（分類→並順）で取得する。 */
    @Query("SELECT q FROM QualificationEntity q LEFT JOIN FETCH q.category WHERE q.active = :active ORDER BY q.category.sortOrder ASC NULLS LAST, q.sortOrder ASC")
    List<QualificationEntity> findAllWithCategoryByActive(@Param("active") boolean active);
}

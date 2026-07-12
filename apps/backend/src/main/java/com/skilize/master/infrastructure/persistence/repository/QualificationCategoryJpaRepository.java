/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格分類マスタの Spring Data JPA リポジトリ。フラット構造（階層なし）の資格分類を取得する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.QualificationCategoryRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.infrastructure.persistence.entity.QualificationCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 資格分類 Spring Data JPA リポジトリ。フラット構造（階層なし）。 */
public interface QualificationCategoryJpaRepository extends JpaRepository<QualificationCategoryEntity, Integer> {
    /** 有効な資格分類を表示順昇順で返す。棚卸入力画面の選択肢に使用。 */
    List<QualificationCategoryEntity> findByActiveTrueOrderBySortOrderAsc();
    /** 全資格分類（有効・無効含む）を表示順昇順で返す。マスタ管理画面に使用。 */
    List<QualificationCategoryEntity> findAllByOrderBySortOrderAsc();
}

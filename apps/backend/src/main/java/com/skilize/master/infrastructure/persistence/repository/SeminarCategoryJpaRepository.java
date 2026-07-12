/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * セミナー分類マスタの Spring Data JPA リポジトリ。自由入力セミナー分類の有効件のみ取得する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.SeminarCategoryRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.infrastructure.persistence.entity.SeminarCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** セミナー分類 Spring Data JPA リポジトリ。自由入力セミナーの分類（ADセミナーとは別系統）。 */
public interface SeminarCategoryJpaRepository extends JpaRepository<SeminarCategoryEntity, Integer> {
    /** 有効なセミナー分類を表示順昇順で返す。 */
    List<SeminarCategoryEntity> findByActiveTrueOrderBySortOrderAsc();
}

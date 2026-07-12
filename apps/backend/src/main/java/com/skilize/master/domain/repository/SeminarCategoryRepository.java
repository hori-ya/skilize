/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * セミナー分類リポジトリインターフェース。永続化の実装詳細を持たない。自由入力セミナーの分類（ADセミナーとは別系統）。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のSeminarCategoryRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.repository;

import com.skilize.master.domain.model.SeminarCategory;

import java.util.List;
import java.util.Optional;

/** セミナー分類リポジトリ。実装は infrastructure.persistence.repository.SeminarCategoryRepositoryImpl。 */
public interface SeminarCategoryRepository {

    /** IDでセミナー分類を取得する。 */
    Optional<SeminarCategory> findById(Integer id);

    /** 有効なセミナー分類を表示順昇順で返す。 */
    List<SeminarCategory> findByActiveTrueOrderBySortOrderAsc();
}

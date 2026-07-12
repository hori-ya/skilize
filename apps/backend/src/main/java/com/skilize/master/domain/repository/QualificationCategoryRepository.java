/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格分類リポジトリインターフェース。永続化の実装詳細を持たない。フラット構造（階層なし）。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のQualificationCategoryRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.repository;

import com.skilize.master.domain.model.QualificationCategory;

import java.util.List;
import java.util.Optional;

/** 資格分類リポジトリ。実装は infrastructure.persistence.repository.QualificationCategoryRepositoryImpl。 */
public interface QualificationCategoryRepository {

    /** IDで資格分類を取得する。 */
    Optional<QualificationCategory> findById(Integer id);

    /** 資格分類を保存する（新規作成・更新の両方に使用）。 */
    QualificationCategory save(QualificationCategory category);

    /** 全資格分類を返す（順不同）。 */
    List<QualificationCategory> findAll();

    /** 有効な資格分類を表示順昇順で返す。棚卸入力画面の選択肢に使用。 */
    List<QualificationCategory> findByActiveTrueOrderBySortOrderAsc();

    /** 全資格分類（有効・無効含む）を表示順昇順で返す。マスタ管理画面に使用。 */
    List<QualificationCategory> findAllByOrderBySortOrderAsc();
}

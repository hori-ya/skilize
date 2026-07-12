/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナー分類リポジトリインターフェース。永続化の実装詳細を持たない。フラット構造（階層なし）。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のAdSeminarCategoryRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.repository;

import com.skilize.master.domain.model.AdSeminarCategory;

import java.util.List;
import java.util.Optional;

/** ADセミナー分類リポジトリ。実装は infrastructure.persistence.repository.AdSeminarCategoryRepositoryImpl。 */
public interface AdSeminarCategoryRepository {

    /** IDでADセミナー分類を取得する。 */
    Optional<AdSeminarCategory> findById(Integer id);

    /** ADセミナー分類を保存する（新規作成・更新の両方に使用）。 */
    AdSeminarCategory save(AdSeminarCategory category);

    /** 全ADセミナー分類を返す（順不同）。 */
    List<AdSeminarCategory> findAll();

    /** 有効なADセミナー分類を表示順昇順で返す。棚卸入力画面の選択肢に使用。 */
    List<AdSeminarCategory> findByActiveTrueOrderBySortOrderAsc();

    /** 全ADセミナー分類（有効・無効含む）を表示順昇順で返す。マスタ管理画面に使用。 */
    List<AdSeminarCategory> findAllByOrderBySortOrderAsc();
}

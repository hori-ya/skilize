/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル分類リポジトリインターフェース。永続化の実装詳細を持たない。最大3階層の自己参照構造を持つ。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のItSkillCategoryRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.repository;

import com.skilize.master.domain.model.ItSkillCategory;

import java.util.List;
import java.util.Optional;

/** ITスキル分類リポジトリ。実装は infrastructure.persistence.repository.ItSkillCategoryRepositoryImpl。 */
public interface ItSkillCategoryRepository {

    /** IDでITスキル分類を取得する。 */
    Optional<ItSkillCategory> findById(Integer id);

    /** ITスキル分類を保存する（新規作成・更新の両方に使用）。 */
    ItSkillCategory save(ItSkillCategory category);

    /** 全ITスキル分類を返す（順不同）。 */
    List<ItSkillCategory> findAll();

    /** 有効な分類を表示順昇順で返す。棚卸入力画面の選択肢に使用。 */
    List<ItSkillCategory> findByActiveTrueOrderBySortOrderAsc();

    /** 指定レベル（1=大分類, 2=中分類, 3=小分類）の有効な分類を返す。 */
    List<ItSkillCategory> findByLevelAndActiveTrueOrderBySortOrderAsc(short level);

    /** 無効な分類を階層レベル昇順・親分類ID昇順・表示順昇順で返す。マスタ管理画面の無効フィルタに使用。 */
    List<ItSkillCategory> findByActiveFalseOrderByLevelAscParentIdAscSortOrderAsc();

    /** 全分類を階層レベル昇順・表示順昇順で返す。レーダーチャート集計（ChartService）に使用。 */
    List<ItSkillCategory> findAllByOrderByLevelAscSortOrderAsc();

    /** 全分類を階層レベル昇順・親分類ID昇順・表示順昇順で返す。マスタ管理画面の一覧表示に使用。 */
    List<ItSkillCategory> findAllByOrderByLevelAscParentIdAscSortOrderAsc();
}

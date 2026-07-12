/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル分類マスタの Spring Data JPA リポジトリ。最大3階層の自己参照構造に対応した各種取得メソッドを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.ItSkillCategoryRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.infrastructure.persistence.entity.ItSkillCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** ITスキル分類 Spring Data JPA リポジトリ。最大3階層の自己参照構造を持つ。 */
public interface ItSkillCategoryJpaRepository extends JpaRepository<ItSkillCategoryEntity, Integer> {
    /** 有効な分類を表示順昇順で返す。棚卸入力画面の選択肢に使用。 */
    List<ItSkillCategoryEntity> findByActiveTrueOrderBySortOrderAsc();
    /** 指定レベル（1=大分類, 2=中分類, 3=小分類）の有効な分類を返す。 */
    List<ItSkillCategoryEntity> findByLevelAndActiveTrueOrderBySortOrderAsc(short level);
    /** 無効な分類を階層レベル昇順・親分類ID昇順・表示順昇順で返す。マスタ管理画面の無効フィルタに使用。 */
    List<ItSkillCategoryEntity> findByActiveFalseOrderByLevelAscParentIdAscSortOrderAsc();
    /** 全分類を階層レベル昇順・表示順昇順で返す。レーダーチャート集計（ChartService）に使用。 */
    List<ItSkillCategoryEntity> findAllByOrderByLevelAscSortOrderAsc();
    /** 全分類を階層レベル昇順・親分類ID昇順・表示順昇順で返す。マスタ管理画面の一覧表示に使用。 */
    List<ItSkillCategoryEntity> findAllByOrderByLevelAscParentIdAscSortOrderAsc();
}

/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル分類マスタのデータアクセスインターフェース。
 * 最大3階層の自己参照構造に対応した各種取得メソッドを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** ITスキル分類リポジトリ。最大3階層の自己参照構造を持つ。 */
public interface ItSkillCategoryRepository extends JpaRepository<ItSkillCategory, Integer> {
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

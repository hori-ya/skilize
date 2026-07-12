/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキルリポジトリインターフェース。永続化の実装詳細を持たない。
 * マスタ管理画面向けの階層ソート付き全件取得と、棚卸入力画面向けの有効スキル取得を提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のItSkillRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.repository;

import com.skilize.master.domain.model.ItSkill;

import java.util.List;
import java.util.Optional;

/** ITスキルリポジトリ。実装は infrastructure.persistence.repository.ItSkillRepositoryImpl。 */
public interface ItSkillRepository {

    /** IDでITスキルを取得する。 */
    Optional<ItSkill> findById(Integer id);

    /** ITスキルを保存する（新規作成・更新の両方に使用）。 */
    ItSkill save(ItSkill itSkill);

    /** 全ITスキルを返す（順不同）。 */
    List<ItSkill> findAll();

    /**
     * 全ITスキルをマスタ管理画面向けソート順（分類1→分類2→分類3→並順）で取得する（有効・無効含む全件）。
     */
    List<ItSkill> findAllOrderByHierarchy();

    /** 有効なITスキルのみをカテゴリ付きで取得する。棚卸入力画面の選択肢に使用（JOIN FETCH で N+1 回避）。 */
    List<ItSkill> findAllActiveWithCategory();

    /**
     * 無効なITスキルをマスタ管理画面向けソート順（分類1→分類2→分類3→並順）で取得する。
     */
    List<ItSkill> findByActiveFalseOrderByHierarchy();
}

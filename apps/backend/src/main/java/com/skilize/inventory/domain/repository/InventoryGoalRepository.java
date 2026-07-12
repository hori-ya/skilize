/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 目標リポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のInventoryGoalRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain.repository;

import com.skilize.inventory.domain.model.InventoryGoal;

import java.util.List;
import java.util.Optional;

/** 目標リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。実装は infrastructure.persistence.repository.InventoryGoalRepositoryImpl。 */
public interface InventoryGoalRepository {

    /** IDで目標を取得する。 */
    Optional<InventoryGoal> findById(Integer id);

    /** 目標を保存する（新規作成・更新の両方に使用）。 */
    InventoryGoal save(InventoryGoal goal);

    /** 目標を一括保存する。 */
    List<InventoryGoal> saveAll(List<InventoryGoal> goals);

    /** 指定棚卸の目標を取得する。 */
    List<InventoryGoal> findByInventoryId(int inventoryId);

    /** 帳票出力用: ITスキル・カテゴリ・親カテゴリ・資格・ADセミナー・ADカテゴリを含めて取得する。 */
    List<InventoryGoal> findByInventoryIdForReport(int inventoryId);

    /** 指定棚卸の目標を全件削除する。全件洗い替え時の DELETE に使用する。 */
    void deleteByInventoryId(int inventoryId);
}

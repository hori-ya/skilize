/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 棚卸リポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のInventoryRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain.repository;

import com.skilize.inventory.domain.model.Inventory;

import java.util.List;
import java.util.Optional;

/** 棚卸リポジトリ。実装は infrastructure.persistence.repository.InventoryRepositoryImpl。 */
public interface InventoryRepository {

    /** IDで棚卸を取得する。 */
    Optional<Inventory> findById(Integer id);

    /** 棚卸を保存する（新規作成・更新の両方に使用）。 */
    Inventory save(Inventory inventory);

    /** 棚卸を年度・ユーザーと一緒に取得する。InventoryService.findById() で使用。 */
    Optional<Inventory> findByIdWithAssociations(int id);

    /** 指定ユーザーの全棚卸を年度情報付きで新しい順に返す。 */
    List<Inventory> findByUserIdWithFiscalYear(int userId);

    /** 指定ユーザーの指定年度の棚卸を返す。今年度棚卸の存在確認に使用。 */
    Optional<Inventory> findByUserIdAndFiscalYearId(int userId, int fiscalYearId);
}

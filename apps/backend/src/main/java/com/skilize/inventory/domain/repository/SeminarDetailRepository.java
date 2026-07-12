/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * セミナー明細リポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のSeminarDetailRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain.repository;

import com.skilize.inventory.domain.model.SeminarDetail;

import java.util.List;

/** セミナー明細リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。実装は infrastructure.persistence.repository.SeminarDetailRepositoryImpl。 */
public interface SeminarDetailRepository {

    /** セミナー明細を一括保存する。 */
    List<SeminarDetail> saveAll(List<SeminarDetail> details);

    /** 指定棚卸のセミナー明細を取得する。 */
    List<SeminarDetail> findByInventoryId(int inventoryId);

    /** 指定棚卸のセミナー明細を全件削除する。全件洗い替え時の DELETE に使用する。 */
    void deleteByInventoryId(int inventoryId);
}

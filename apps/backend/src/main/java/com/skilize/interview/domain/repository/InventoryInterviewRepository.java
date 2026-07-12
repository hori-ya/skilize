/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談メモヘッダーリポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のInventoryInterviewRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.domain.repository;

import com.skilize.interview.domain.model.InventoryInterview;

import java.util.Optional;

/**
 * 面談メモヘッダーリポジトリ。実装は infrastructure.persistence.repository.InventoryInterviewRepositoryImpl。
 * 面談メモは「棚卸 × 面談者」の組み合わせで一意。同一棚卸に複数の TL が面談した場合は複数レコードになる。
 */
public interface InventoryInterviewRepository {

    /** 面談メモを保存する（新規作成・更新の両方に使用）。 */
    InventoryInterview save(InventoryInterview interview);

    /** 指定棚卸・指定面談者の面談メモを返す。 */
    Optional<InventoryInterview> findByInventoryIdAndInterviewerId(int inventoryId, int interviewerId);
}

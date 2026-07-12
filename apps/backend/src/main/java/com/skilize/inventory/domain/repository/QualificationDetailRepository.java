/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格明細リポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のQualificationDetailRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain.repository;

import com.skilize.inventory.domain.model.QualificationDetail;
import com.skilize.master.domain.model.Qualification;

import java.util.List;

/** 資格明細リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。実装は infrastructure.persistence.repository.QualificationDetailRepositoryImpl。 */
public interface QualificationDetailRepository {

    /** 資格明細を一括保存する。 */
    List<QualificationDetail> saveAll(List<QualificationDetail> details);

    /** 指定棚卸の資格明細を取得する。 */
    List<QualificationDetail> findByInventoryId(int inventoryId);

    /** 指定棚卸の資格明細を全件削除する。全件洗い替え時の DELETE に使用する。 */
    void deleteByInventoryId(int inventoryId);

    /** カスタム資格名のうち qualifications マスタに未登録のものを使用件数付きで返す。 */
    List<Object[]> findCustomUnregisteredQualificationNames();

    /** 昇格後、同名カスタム資格明細をマスタ資格へ紐付ける。 */
    void linkToMasterQualification(String customName, Qualification qualification);
}

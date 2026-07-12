/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル明細リポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のItSkillDetailRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain.repository;

import com.skilize.inventory.domain.model.ItSkillDetail;
import com.skilize.master.domain.model.ItSkill;

import java.util.List;
import java.util.Optional;

/** ITスキル明細リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。実装は infrastructure.persistence.repository.ItSkillDetailRepositoryImpl。 */
public interface ItSkillDetailRepository {

    /** IDでITスキル明細を取得する。 */
    Optional<ItSkillDetail> findById(Integer id);

    /** ITスキル明細を保存する（新規作成・更新の両方に使用）。 */
    ItSkillDetail save(ItSkillDetail detail);

    /** ITスキル明細を一括保存する。 */
    List<ItSkillDetail> saveAll(List<ItSkillDetail> details);

    /** 指定棚卸のITスキル明細を取得する。 */
    List<ItSkillDetail> findByInventoryId(int inventoryId);

    /** 帳票出力用: カテゴリ・親カテゴリを含めて取得する。 */
    List<ItSkillDetail> findByInventoryIdWithCategories(int inventoryId);

    /** 指定棚卸のITスキル明細を全件削除する。全件洗い替え時の DELETE に使用する。 */
    void deleteByInventoryId(int inventoryId);

    /** カスタムスキル名のうち it_skills マスタに未登録のものを使用件数付きで返す。 */
    List<Object[]> findCustomUnregisteredSkillNames();

    /** 昇格後、同名カスタムスキル明細をマスタスキルへ紐付ける。 */
    void linkToMasterSkill(String customName, ItSkill skill);
}

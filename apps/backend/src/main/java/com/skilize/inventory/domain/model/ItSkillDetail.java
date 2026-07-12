/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル棚卸明細ドメインモデル。1棚卸あたり複数行のITスキル採点を管理する。
 * マスタスキルとカスタムスキル名のいずれか一方が必ず設定される。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.ItSkillDetailEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain.model;

import com.skilize.master.domain.model.ItSkill;
import com.skilize.master.domain.model.SkillLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ITスキル棚卸明細。1棚卸あたり複数行のITスキル採点を管理する。JPA/Springに依存しない純粋なドメインモデル。
 * マスタスキルまたはカスタムスキル名のいずれか一方が必ず設定される（両方 null は不可）。
 *
 * 項目（論理名）:
 *   棚卸ID        - 紐付く棚卸の内部ID（棚卸自体の詳細情報は不要なためIDのみ保持）
 *   ITスキル      - マスタ参照スキル。カスタムスキルの場合は null
 *   カスタムスキル名 - マスタ未登録のスキル名。TL がマスタ昇格できる
 *   スキルレベル   - 採点レベル（skill_levels テーブル参照）
 *   備考          - 採点根拠・補足説明。面談メモの明細ノートとは別物
 */
@Getter
@NoArgsConstructor
public class ItSkillDetail {

    private Integer id;
    private Integer inventoryId;
    private ItSkill itSkill;
    private String customSkillName;
    private SkillLevel skillLevel;
    private String remarks;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * ITスキル明細を新規作成する。
     *
     * @param inventory       紐付ける棚卸
     * @param itSkill         ITスキルマスタ参照（カスタムスキルの場合は null）
     * @param customSkillName カスタムスキル名（itSkill が null の場合に使用）
     * @param skillLevel      スキルレベルマスタ参照
     * @param remarks         備考
     * @return 新規作成したITスキル明細（未永続化）
     */
    public static ItSkillDetail create(Inventory inventory, ItSkill itSkill, String customSkillName,
                                       SkillLevel skillLevel, String remarks) {
        ItSkillDetail d = new ItSkillDetail();
        d.inventoryId = inventory.getId();
        d.itSkill = itSkill;
        d.customSkillName = customSkillName;
        d.skillLevel = skillLevel;
        d.remarks = remarks;
        return d;
    }

    /**
     * 永続化済みの状態からITスキル明細を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static ItSkillDetail reconstruct(Integer id, Integer inventoryId, ItSkill itSkill, String customSkillName,
                                            SkillLevel skillLevel, String remarks,
                                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        ItSkillDetail d = new ItSkillDetail();
        d.id = id;
        d.inventoryId = inventoryId;
        d.itSkill = itSkill;
        d.customSkillName = customSkillName;
        d.skillLevel = skillLevel;
        d.remarks = remarks;
        d.createdAt = createdAt;
        d.updatedAt = updatedAt;
        return d;
    }

    /**
     * 備考を更新する。スキルレベルを変えずに備考のみ部分更新する際に使用する。
     */
    public void updateRemarks(String remarks) {
        this.remarks = remarks;
    }
}

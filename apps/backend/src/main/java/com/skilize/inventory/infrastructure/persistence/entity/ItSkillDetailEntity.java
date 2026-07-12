/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル棚卸明細JPAエンティティ。it_skill_details テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.ItSkillDetail から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.entity;

import com.skilize.master.infrastructure.persistence.entity.ItSkillEntity;
import com.skilize.master.infrastructure.persistence.entity.SkillLevelEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** ITスキル棚卸明細JPAエンティティ。マスタスキルとカスタムスキル名のいずれか一方が必ず設定される。 */
@Entity
@Table(name = "it_skill_details")
@Getter
@NoArgsConstructor
public class ItSkillDetailEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 棚卸
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private InventoryEntity inventory;

    // ITスキル（カスタムスキルの場合は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "it_skill_id")
    private ItSkillEntity itSkill;

    // カスタムスキル名（itSkill が null の場合に使用）
    @Column(name = "custom_skill_name")
    private String customSkillName;

    // スキルレベル
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_level_id", nullable = false)
    private SkillLevelEntity skillLevel;

    // 備考
    private String remarks;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static ItSkillDetailEntity create(InventoryEntity inventory, ItSkillEntity itSkill, String customSkillName,
                                             SkillLevelEntity skillLevel, String remarks) {
        ItSkillDetailEntity d = new ItSkillDetailEntity();
        d.inventory = inventory;
        d.itSkill = itSkill;
        d.customSkillName = customSkillName;
        d.skillLevel = skillLevel;
        d.remarks = remarks;
        return d;
    }

    public void updateRemarks(String remarks) {
        this.remarks = remarks;
    }
}

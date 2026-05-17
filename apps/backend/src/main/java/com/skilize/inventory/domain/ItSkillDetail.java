package com.skilize.inventory.domain;

import com.skilize.master.domain.ItSkill;
import com.skilize.master.domain.SkillLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ITスキル棚卸明細。1棚卸あたり複数行のITスキル採点を管理する。
 * マスタスキルまたはカスタムスキル名のいずれか一方が必ず設定される（両方 null は不可）。
 *
 * 項目（論理名）:
 *   ITスキル      - マスタ参照スキル。カスタムスキルの場合は null
 *   カスタムスキル名 - マスタ未登録のスキル名。TL がマスタ昇格できる
 *   スキルレベル   - 採点レベル（skill_levels テーブル参照）
 *   備考          - 採点根拠・補足説明。面談メモの明細ノートとは別物
 */
@Entity
@Table(name = "it_skill_details")
@Getter
@NoArgsConstructor
public class ItSkillDetail {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 棚卸
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    // ITスキル（カスタムスキルの場合は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "it_skill_id")
    private ItSkill itSkill;

    // カスタムスキル名（itSkill が null の場合に使用）
    @Column(name = "custom_skill_name")
    private String customSkillName;

    // スキルレベル
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_level_id", nullable = false)
    private SkillLevel skillLevel;

    // 備考（採点根拠・面談ノートとは別）
    private String remarks;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static ItSkillDetail create(Inventory inventory, ItSkill itSkill, String customSkillName,
                                       SkillLevel skillLevel, String remarks) {
        ItSkillDetail d = new ItSkillDetail();
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

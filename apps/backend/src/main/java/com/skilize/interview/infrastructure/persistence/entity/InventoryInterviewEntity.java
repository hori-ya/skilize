/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談メモヘッダーJPAエンティティ。inventory_interviews テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.InventoryInterview から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.infrastructure.persistence.entity;

import com.skilize.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.skilize.user.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** 面談メモヘッダーJPAエンティティ。1棚卸につき面談者（TL/ADMIN）1名分のレコードが作成される。 */
@Entity
@Table(name = "inventory_interviews")
@Getter
@NoArgsConstructor
public class InventoryInterviewEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 棚卸
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private InventoryEntity inventory;

    // 面談者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private UserEntity interviewer;

    // 全体備忘録
    @Column(name = "general_note")
    private String generalNote;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static InventoryInterviewEntity create(InventoryEntity inventory, UserEntity interviewer, String generalNote) {
        InventoryInterviewEntity e = new InventoryInterviewEntity();
        e.inventory = inventory;
        e.interviewer = interviewer;
        e.generalNote = generalNote;
        return e;
    }

    public void update(String generalNote) {
        this.generalNote = generalNote;
    }
}

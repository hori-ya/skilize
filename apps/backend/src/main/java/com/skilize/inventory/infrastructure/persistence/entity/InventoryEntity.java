/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 棚卸ヘッダーJPAエンティティ。inventories テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.Inventory から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.entity;

import com.skilize.fiscalyear.infrastructure.persistence.entity.FiscalYearEntity;
import com.skilize.inventory.domain.model.InventoryStatus;
import com.skilize.user.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** 棚卸ヘッダーJPAエンティティ。ユーザーと年度の組み合わせで 1 件のみ存在する（ユーザー×年度でユニーク）。 */
@Entity
@Table(name = "inventories")
@Getter
@NoArgsConstructor
public class InventoryEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ユーザー
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // 年度
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYearEntity fiscalYear;

    // ステータス
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status;

    // 提出日時
    private OffsetDateTime submittedAt;
    // 前回目標振り返り完了日時
    private OffsetDateTime goalReviewCompletedAt;
    // 目標設定完了日時
    private OffsetDateTime goalCompletedAt;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static InventoryEntity create(UserEntity user, FiscalYearEntity fiscalYear) {
        InventoryEntity inv = new InventoryEntity();
        inv.user = user;
        inv.fiscalYear = fiscalYear;
        inv.status = InventoryStatus.DRAFT;
        return inv;
    }

    public void submit() {
        this.status = InventoryStatus.PENDING_GOAL;
        this.submittedAt = OffsetDateTime.now();
    }

    public void completeGoalReview() {
        this.goalReviewCompletedAt = OffsetDateTime.now();
    }

    public void completeGoal() {
        this.status = InventoryStatus.COMPLETED;
        this.goalCompletedAt = OffsetDateTime.now();
    }

    /** ドメインモデル側で計算済みの状態をそのまま反映する（タイムスタンプを再計算しない）。 */
    public void applyState(InventoryStatus status, OffsetDateTime submittedAt,
                           OffsetDateTime goalReviewCompletedAt, OffsetDateTime goalCompletedAt) {
        this.status = status;
        this.submittedAt = submittedAt;
        this.goalReviewCompletedAt = goalReviewCompletedAt;
        this.goalCompletedAt = goalCompletedAt;
    }
}

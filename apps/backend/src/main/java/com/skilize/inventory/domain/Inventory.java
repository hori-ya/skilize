/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 棚卸ヘッダーエンティティ。ユーザーと年度の組み合わせで 1 件のみ存在する棚卸を表す。
 * ステータス遷移（DRAFT → PENDING_GOAL → COMPLETED）をドメインメソッドで管理する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.domain;

import com.skilize.fiscalyear.domain.FiscalYear;
import com.skilize.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 棚卸ヘッダー。ユーザーと年度の組み合わせで 1 件のみ存在する（ユーザー×年度でユニーク）。
 * ステータス遷移は InventoryStatus を参照。タイムスタンプはドメインメソッドで管理する。
 *
 * 項目（論理名）:
 *   ステータス             - DRAFT / PENDING_GOAL / COMPLETED
 *   提出日時               - 棚卸提出時に設定（submit() 呼び出し時）
 *   前回目標振り返り完了日時 - NULL かつ前年度目標あり → ログイン時に振り返り画面へ誘導
 *   目標設定完了日時        - 目標完了操作（completeGoal()）時に設定
 */
@Entity
@Table(name = "inventories")
@Getter
@NoArgsConstructor
public class Inventory {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ユーザー
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 年度
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

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

    /**
     * 棚卸を新規作成する。ステータスは DRAFT で初期化される。
     *
     * @param user       棚卸を所有するユーザー
     * @param fiscalYear 対象年度
     * @return 新規作成した棚卸エンティティ（未永続化）
     */
    public static Inventory create(User user, FiscalYear fiscalYear) {
        Inventory inv = new Inventory();
        inv.user = user;
        inv.fiscalYear = fiscalYear;
        inv.status = InventoryStatus.DRAFT;
        return inv;
    }

    /**
     * 棚卸を提出する。ステータスを PENDING_GOAL に変更し、提出日時を現在日時で設定する。
     */
    public void submit() {
        this.status = InventoryStatus.PENDING_GOAL;
        this.submittedAt = OffsetDateTime.now();
    }

    /**
     * 目標振り返りを完了させる。ステータス変更はなく、完了日時のみ現在日時で記録する。
     */
    public void completeGoalReview() {
        // ステータス変更はなく完了日時のみ記録する（目標振り返りは任意フロー）
        this.goalReviewCompletedAt = OffsetDateTime.now();
    }

    /**
     * 目標設定を完了させる。ステータスを COMPLETED に変更し、目標設定完了日時を現在日時で設定する。
     */
    public void completeGoal() {
        this.status = InventoryStatus.COMPLETED;
        this.goalCompletedAt = OffsetDateTime.now();
    }
}

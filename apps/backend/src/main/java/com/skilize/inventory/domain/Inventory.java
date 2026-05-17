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

    public static Inventory create(User user, FiscalYear fiscalYear) {
        Inventory inv = new Inventory();
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
        // ステータス変更はなく完了日時のみ記録する（目標振り返りは任意フロー）
        this.goalReviewCompletedAt = OffsetDateTime.now();
    }

    public void completeGoal() {
        this.status = InventoryStatus.COMPLETED;
        this.goalCompletedAt = OffsetDateTime.now();
    }
}

package com.skilize.inventory.domain;

import com.skilize.master.domain.AdSeminar;
import com.skilize.master.domain.ItSkill;
import com.skilize.master.domain.Qualification;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 目標設定。今年度の棚卸に設定する翌年度に向けた目標。
 * 目標カテゴリに応じて参照するマスタが変わる:
 *   ITスキル目標   → マスタITスキルまたはカスタム目標名
 *   資格目標       → マスタ資格またはカスタム目標名
 *   AD目標         → ADセミナーマスタ（必須。カスタム目標なし）
 *
 * 項目（論理名）:
 *   目標カテゴリ     - ITスキル / 資格 / AD の3種類
 *   ITスキル         - ITスキル目標のマスタ参照（カスタム目標の場合は null）
 *   資格             - 資格目標のマスタ参照（カスタム目標の場合は null）
 *   ADセミナー       - AD目標では必須。ITスキル・資格目標では null
 *   カスタム目標名   - マスタ未登録のITスキル・資格目標の自由テキスト名
 *   達成・予定時期   - DATE型だが常に月初日(1日)で保存
 *   目標理由         - 目標設定の理由
 *   達成状況         - 翌年度振り返り時の達成状況（ACHIEVED / PARTIAL / NOT_ACHIEVED。振り返り前は null）
 *   振り返りコメント - 翌年度振り返りコメント
 *
 * 目標完了の件数条件（InventoryService.completeGoal で検証）:
 *   ITスキル + 資格 の合計 ≥1 件 AND AD ≥2 件
 */
@Entity
@Table(name = "inventory_goals")
@Getter
@NoArgsConstructor
public class InventoryGoal {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 棚卸
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    // 目標カテゴリ（ITスキル / 資格 / AD）
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_category", nullable = false)
    private GoalCategory goalCategory;

    // ITスキル（ITスキル目標のマスタ参照。それ以外は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "it_skill_id")
    private ItSkill itSkill;

    // 資格（資格目標のマスタ参照。それ以外は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qualification_id")
    private Qualification qualification;

    // ADセミナー（AD目標では必須。それ以外は null）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_seminar_id")
    private AdSeminar adSeminar;

    // カスタム目標名（マスタ未登録のITスキル・資格目標）
    @Column(name = "custom_name")
    private String customName;

    // 達成・予定時期（月初日で保存）
    @Column(name = "target_period", nullable = false)
    private LocalDate targetPeriod;

    // 目標理由
    private String reason;

    // 達成状況（振り返り前は null）
    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_status")
    private AchievementStatus achievementStatus;

    // 振り返りコメント
    @Column(name = "review_note")
    private String reviewNote;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static InventoryGoal create(Inventory inventory, GoalCategory goalCategory,
                                       ItSkill itSkill, Qualification qualification,
                                       AdSeminar adSeminar, String customName,
                                       LocalDate targetPeriod, String reason) {
        InventoryGoal g = new InventoryGoal();
        g.inventory = inventory;
        g.goalCategory = goalCategory;
        g.itSkill = itSkill;
        g.qualification = qualification;
        g.adSeminar = adSeminar;
        g.customName = customName;
        g.targetPeriod = targetPeriod;
        g.reason = reason;
        return g;
    }

    public void updateReview(AchievementStatus achievementStatus, String reviewNote) {
        this.achievementStatus = achievementStatus;
        this.reviewNote = reviewNote;
    }
}

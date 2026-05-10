package com.skilize.domain.inventory;

import com.skilize.domain.master.AdSeminar;
import com.skilize.domain.master.ItSkill;
import com.skilize.domain.master.Qualification;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_goals")
@Getter
@NoArgsConstructor
public class InventoryGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_category", nullable = false)
    private GoalCategory goalCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "it_skill_id")
    private ItSkill itSkill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qualification_id")
    private Qualification qualification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_seminar_id")
    private AdSeminar adSeminar;

    @Column(name = "custom_name")
    private String customName;

    @Column(name = "target_period", nullable = false)
    private LocalDate targetPeriod;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_status")
    private AchievementStatus achievementStatus;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

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

package com.skilize.inventory.domain;

import com.skilize.fiscalyear.domain.FiscalYear;
import com.skilize.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inventories")
@Getter
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status;

    private OffsetDateTime submittedAt;
    private OffsetDateTime goalReviewCompletedAt;
    private OffsetDateTime goalCompletedAt;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

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
        this.goalReviewCompletedAt = OffsetDateTime.now();
    }

    public void completeGoal() {
        this.status = InventoryStatus.COMPLETED;
        this.goalCompletedAt = OffsetDateTime.now();
    }
}

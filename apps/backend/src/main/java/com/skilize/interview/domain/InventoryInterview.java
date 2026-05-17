package com.skilize.interview.domain;

import com.skilize.inventory.domain.Inventory;
import com.skilize.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_interviews")
@Getter
@NoArgsConstructor
public class InventoryInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Column(name = "general_note")
    private String generalNote;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static InventoryInterview create(Inventory inventory, User interviewer, String generalNote) {
        InventoryInterview e = new InventoryInterview();
        e.inventory = inventory;
        e.interviewer = interviewer;
        e.generalNote = generalNote;
        return e;
    }

    public void update(String generalNote) {
        this.generalNote = generalNote;
    }
}

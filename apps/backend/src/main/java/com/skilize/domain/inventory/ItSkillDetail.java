package com.skilize.domain.inventory;

import com.skilize.domain.master.ItSkill;
import com.skilize.domain.master.SkillLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "it_skill_details")
@Getter
@NoArgsConstructor
public class ItSkillDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "it_skill_id")
    private ItSkill itSkill;

    @Column(name = "custom_skill_name")
    private String customSkillName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_level_id", nullable = false)
    private SkillLevel skillLevel;

    private String remarks;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

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

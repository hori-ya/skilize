package com.skilize.domain.master;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "skill_levels")
@Getter
@NoArgsConstructor
public class SkillLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Short levelValue;

    @Column(nullable = false)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static SkillLevel create(Short levelValue, String description) {
        SkillLevel s = new SkillLevel();
        s.levelValue = levelValue;
        s.description = description;
        s.active = true;
        return s;
    }

    public void update(Short levelValue, String description, boolean active) {
        this.levelValue = levelValue;
        this.description = description;
        this.active = active;
    }
}

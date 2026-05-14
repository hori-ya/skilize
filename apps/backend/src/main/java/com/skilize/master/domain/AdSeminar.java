package com.skilize.master.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ad_seminars")
@Getter
@NoArgsConstructor
public class AdSeminar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private AdSeminarCategory category;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static AdSeminar create(AdSeminarCategory category, String name, String description, int sortOrder) {
        AdSeminar a = new AdSeminar();
        a.category = category;
        a.name = name;
        a.description = description;
        a.sortOrder = sortOrder;
        a.active = true;
        return a;
    }

    public void update(AdSeminarCategory category, String name, String description, int sortOrder, boolean active) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

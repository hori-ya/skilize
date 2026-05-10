package com.skilize.domain.master;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "qualifications")
@Getter
@NoArgsConstructor
public class Qualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private QualificationCategory category;

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

    public static Qualification create(QualificationCategory category, String name, String description, int sortOrder) {
        Qualification q = new Qualification();
        q.category = category;
        q.name = name;
        q.description = description;
        q.sortOrder = sortOrder;
        q.active = true;
        return q;
    }

    public void update(QualificationCategory category, String name, String description, int sortOrder, boolean active) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

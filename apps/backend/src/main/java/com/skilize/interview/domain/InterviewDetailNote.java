package com.skilize.interview.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "interview_detail_notes")
@Getter
@NoArgsConstructor
public class InterviewDetailNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InventoryInterview interview;

    @Enumerated(EnumType.STRING)
    @Column(name = "detail_type", nullable = false)
    private DetailType detailType;

    @Column(name = "detail_id", nullable = false)
    private Integer detailId;

    @Column(nullable = false)
    private String note;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static InterviewDetailNote create(InventoryInterview interview,
                                              DetailType detailType, Integer detailId, String note) {
        InterviewDetailNote e = new InterviewDetailNote();
        e.interview = interview;
        e.detailType = detailType;
        e.detailId = detailId;
        e.note = note;
        return e;
    }
}

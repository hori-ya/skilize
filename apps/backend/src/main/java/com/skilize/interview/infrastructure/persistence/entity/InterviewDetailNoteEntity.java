/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談メモ明細JPAエンティティ。interview_detail_notes テーブルとのマッピングを担う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.InterviewDetailNote から分離（JPAアノテーションはこちらにのみ残す）
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.infrastructure.persistence.entity;

import com.skilize.interview.domain.model.DetailType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** 面談メモ明細JPAエンティティ。明細種別と明細IDの組み合わせで対象行をポリモーフィックに参照する。 */
@Entity
@Table(name = "interview_detail_notes")
@Getter
@NoArgsConstructor
public class InterviewDetailNoteEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 面談メモヘッダー
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InventoryInterviewEntity interview;

    // 明細種別
    @Enumerated(EnumType.STRING)
    @Column(name = "detail_type", nullable = false)
    private DetailType detailType;

    // 明細ID
    @Column(name = "detail_id", nullable = false)
    private Integer detailId;

    // ノート
    @Column(nullable = false)
    private String note;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static InterviewDetailNoteEntity create(InventoryInterviewEntity interview,
                                                    DetailType detailType, Integer detailId, String note) {
        InterviewDetailNoteEntity e = new InterviewDetailNoteEntity();
        e.interview = interview;
        e.detailType = detailType;
        e.detailId = detailId;
        e.note = note;
        return e;
    }
}

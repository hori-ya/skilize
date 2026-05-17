package com.skilize.interview.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 面談メモ明細。棚卸の各明細行（ITスキル・資格・セミナー）に紐づく面談コメント。
 * 明細種別と明細IDの組み合わせでどの明細行へのコメントかを特定する（ポリモーフィック参照）。
 *
 * 項目（論理名）:
 *   面談メモ     - 親となる面談メモヘッダー
 *   明細種別     - ITスキル / 資格 / セミナー の3種類（DetailType 参照）
 *   明細ID       - 対象明細行の内部ID（明細種別に応じてテーブルが変わる）
 *   ノート       - 当該明細行に対する面談コメント（必須）
 */
@Entity
@Table(name = "interview_detail_notes")
@Getter
@NoArgsConstructor
public class InterviewDetailNote {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 面談メモヘッダー
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InventoryInterview interview;

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

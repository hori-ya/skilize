package com.skilize.interview.domain;

import com.skilize.inventory.domain.Inventory;
import com.skilize.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 面談メモヘッダー。TL/ADMIN が棚卸に対して記録する面談記録の親エンティティ。
 * 1棚卸につき面談者（TL/ADMIN）1名分のレコードが作成される。明細メモは InterviewDetailNote を参照。
 *
 * 項目（論理名）:
 *   棚卸         - 面談対象の棚卸（ユーザー×年度）
 *   面談者       - メモを記録したTL/ADMINユーザー
 *   全体備忘録   - 棚卸全体に対するメモ（各明細ノートとは別の自由テキスト）
 */
@Entity
@Table(name = "inventory_interviews")
@Getter
@NoArgsConstructor
public class InventoryInterview {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 棚卸
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    // 面談者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    // 全体備忘録
    @Column(name = "general_note")
    private String generalNote;

    // 作成日時
    @Column(insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 更新日時
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

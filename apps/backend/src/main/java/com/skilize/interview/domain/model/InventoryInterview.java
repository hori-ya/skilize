/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談メモヘッダードメインモデル。TL/ADMIN が棚卸に対して記録する面談記録の親エンティティ。
 * 1棚卸につき面談者1名分のレコードを持ち、明細ノートは InterviewDetailNote を参照する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.InventoryInterviewEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.domain.model;

import com.skilize.user.domain.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 面談メモヘッダー。TL/ADMIN が棚卸に対して記録する面談記録の親エンティティ。JPA/Springに依存しない純粋なドメインモデル。
 * 1棚卸につき面談者（TL/ADMIN）1名分のレコードが作成される。明細メモは InterviewDetailNote を参照。
 *
 * 項目（論理名）:
 *   棚卸ID       - 面談対象の棚卸の内部ID（棚卸自体の詳細情報は不要なためIDのみ保持）
 *   面談者       - メモを記録したTL/ADMINユーザー
 *   全体備忘録   - 棚卸全体に対するメモ（各明細ノートとは別の自由テキスト）
 */
@Getter
@NoArgsConstructor
public class InventoryInterview {

    private Integer id;
    private Integer inventoryId;
    private User interviewer;
    private String generalNote;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static InventoryInterview create(Integer inventoryId, User interviewer, String generalNote) {
        InventoryInterview e = new InventoryInterview();
        e.inventoryId = inventoryId;
        e.interviewer = interviewer;
        e.generalNote = generalNote;
        return e;
    }

    /**
     * 永続化済みの状態から面談メモヘッダーを復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static InventoryInterview reconstruct(Integer id, Integer inventoryId, User interviewer, String generalNote,
                                                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        InventoryInterview e = new InventoryInterview();
        e.id = id;
        e.inventoryId = inventoryId;
        e.interviewer = interviewer;
        e.generalNote = generalNote;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }

    public void update(String generalNote) {
        this.generalNote = generalNote;
    }
}

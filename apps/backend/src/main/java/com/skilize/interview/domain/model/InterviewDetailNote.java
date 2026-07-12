/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談メモ明細ドメインモデル。棚卸の各明細行（ITスキル・資格・セミナー・目標）に紐づく面談コメントを管理する。
 * 明細種別（DetailType）と明細IDの組み合わせで対象行をポリモーフィックに参照する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.InterviewDetailNoteEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 面談メモ明細。棚卸の各明細行（ITスキル・資格・セミナー）に紐づく面談コメント。JPA/Springに依存しない純粋なドメインモデル。
 * 明細種別と明細IDの組み合わせでどの明細行へのコメントかを特定する（ポリモーフィック参照）。
 *
 * 項目（論理名）:
 *   面談メモID   - 親となる面談メモヘッダーの内部ID
 *   明細種別     - ITスキル / 資格 / セミナー の3種類（DetailType 参照）
 *   明細ID       - 対象明細行の内部ID（明細種別に応じてテーブルが変わる）
 *   ノート       - 当該明細行に対する面談コメント（必須）
 */
@Getter
@NoArgsConstructor
public class InterviewDetailNote {

    private Integer id;
    private Integer interviewId;
    private DetailType detailType;
    private Integer detailId;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static InterviewDetailNote create(InventoryInterview interview,
                                              DetailType detailType, Integer detailId, String note) {
        InterviewDetailNote e = new InterviewDetailNote();
        e.interviewId = interview.getId();
        e.detailType = detailType;
        e.detailId = detailId;
        e.note = note;
        return e;
    }

    /**
     * 永続化済みの状態から面談メモ明細を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static InterviewDetailNote reconstruct(Integer id, Integer interviewId, DetailType detailType,
                                                  Integer detailId, String note,
                                                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        InterviewDetailNote e = new InterviewDetailNote();
        e.id = id;
        e.interviewId = interviewId;
        e.detailType = detailType;
        e.detailId = detailId;
        e.note = note;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }
}

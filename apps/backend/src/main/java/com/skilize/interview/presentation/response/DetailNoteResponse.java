/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談明細ノートレスポンスクラス。明細種別・明細ID・メモ内容を返し、
 * フロントエンドが棚卸明細行ごとの面談コメントを表示するために使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.presentation.response;

import com.skilize.interview.domain.DetailType;
import com.skilize.interview.domain.InterviewDetailNote;

/**
 * 面談明細ノートレスポンス。detailType + detailId で対象の棚卸明細（ITスキル・資格・セミナー・目標）を識別する。
 */
public record DetailNoteResponse(
        int id,
        DetailType detailType,
        Integer detailId,
        String note
) {
    /** エンティティからレスポンスに変換するファクトリメソッド。 */
    public static DetailNoteResponse from(InterviewDetailNote note) {
        return new DetailNoteResponse(note.getId(), note.getDetailType(), note.getDetailId(), note.getNote());
    }
}

/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談メモレスポンスクラス。面談ヘッダー情報（全体備忘録・面談者）と明細ノート一覧を合わせて返す。
 * GET/PUT /api/interviews/inventory/{inventoryId} のレスポンスとして使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.presentation.response;

import com.skilize.interview.domain.model.InventoryInterview;

import java.util.List;

/**
 * 面談メモレスポンス。ヘッダー情報（全体備忘録・面談者）と明細ノート一覧を合わせて返す。
 */
public record InterviewResponse(
        int id,
        int inventoryId,
        int interviewerId,
        String interviewerName,
        String generalNote,
        List<DetailNoteResponse> detailNotes
) {
    /** エンティティと明細ノートリストからレスポンスを生成するファクトリメソッド。 */
    public static InterviewResponse from(InventoryInterview interview, int inventoryId, List<DetailNoteResponse> detailNotes) {
        return new InterviewResponse(
                interview.getId(),
                inventoryId,
                interview.getInterviewer().getId(),
                interview.getInterviewer().getName(),
                interview.getGeneralNote(),
                detailNotes
        );
    }
}

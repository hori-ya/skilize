package com.skilize.interview.presentation;

import com.skilize.interview.domain.InventoryInterview;

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
    /** エンティティと明細ノートリストから DTO を生成するファクトリメソッド。 */
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

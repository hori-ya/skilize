package com.skilize.interview.presentation;

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
    /** エンティティから DTO に変換するファクトリメソッド。 */
    public static DetailNoteResponse from(InterviewDetailNote note) {
        return new DetailNoteResponse(note.getId(), note.getDetailType(), note.getDetailId(), note.getNote());
    }
}

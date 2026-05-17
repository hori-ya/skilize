package com.skilize.interview.presentation;

import com.skilize.interview.domain.DetailType;
import com.skilize.interview.domain.InterviewDetailNote;

public record DetailNoteResponse(
        int id,
        DetailType detailType,
        Integer detailId,
        String note
) {
    public static DetailNoteResponse from(InterviewDetailNote note) {
        return new DetailNoteResponse(note.getId(), note.getDetailType(), note.getDetailId(), note.getNote());
    }
}

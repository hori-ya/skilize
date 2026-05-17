package com.skilize.interview.presentation;

import com.skilize.interview.domain.InventoryInterview;

import java.util.List;

public record InterviewResponse(
        int id,
        int inventoryId,
        int interviewerId,
        String interviewerName,
        String generalNote,
        List<DetailNoteResponse> detailNotes
) {
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

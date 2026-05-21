package com.skilize.interview.presentation.request;

import com.skilize.interview.domain.DetailType;
import jakarta.validation.constraints.NotNull;

/**
 * 面談明細ノートリクエスト。detailType + detailId で対象明細を特定し、note でメモ内容を送信する。
 * note は null 許容（空のメモとして保存される）。
 */
public record DetailNoteRequest(
        @NotNull DetailType detailType,
        @NotNull Integer detailId,
        String note
) {}

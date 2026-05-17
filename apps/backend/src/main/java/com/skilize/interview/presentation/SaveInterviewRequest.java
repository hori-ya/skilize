package com.skilize.interview.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 面談メモ保存リクエスト。全体備忘録（generalNote）と明細ノート一覧（detailNotes）をまとめて送信する。
 * detailNotes は null 不可（空リスト [] は許容）。@Valid で各 DetailNoteRequest もバリデーションする。
 */
public record SaveInterviewRequest(
        String generalNote,
        @NotNull @Valid List<DetailNoteRequest> detailNotes
) {}

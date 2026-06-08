/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談メモ保存リクエストクラス。全体備忘録と明細ノート一覧をまとめて送信するリクエスト本文を定義する。
 * PUT /api/interviews/inventory/{inventoryId} で使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.presentation.request;

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

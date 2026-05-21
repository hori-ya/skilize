package com.skilize.interview.application.command;

import com.skilize.interview.domain.DetailType;

/**
 * 面談メモ明細1件の保存コマンド。PUT /api/interviews/inventory/{inventoryId} のリクエスト内アイテム単位。
 * ITスキル・資格・セミナーの各明細に対してメモを紐付けるために使用する。
 *
 * @param detailType 明細種別（IT_SKILL / QUALIFICATION / SEMINAR）
 * @param detailId   対象明細の内部PK
 * @param note       メモ本文（null または空文字で削除）
 */
public record DetailNoteCommand(DetailType detailType, Integer detailId, String note) {}

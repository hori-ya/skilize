package com.skilize.inventory.presentation.request;

/**
 * 棚卸新規作成リクエスト。POST /api/inventories のリクエストボディ。
 * 同一ユーザー・同一年度の棚卸は1件のみ作成可能（重複時は 409 Conflict）。
 *
 * @param fiscalYearId 棚卸を作成する年度の内部 PK
 */
public record CreateInventoryRequest(int fiscalYearId) {}

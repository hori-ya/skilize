package com.skilize.inventory.presentation.request;

/**
 * 備考部分更新リクエスト。PATCH /api/inventories/{id}/it-skill-details/{detailId} のリクエストボディ。
 * スキルレベルを変更せず備考欄だけを更新する際に使用する。
 *
 * @param remarks 更新後の備考（null を送ると備考を空にする）
 */
public record RemarksPatchRequest(String remarks) {}

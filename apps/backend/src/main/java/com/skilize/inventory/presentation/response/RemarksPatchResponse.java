package com.skilize.inventory.presentation.response;

/**
 * 備考部分更新レスポンス。PATCH /api/inventories/{id}/it-skill-details/{detailId} のレスポンスに使用する。
 *
 * @param id      更新した明細の内部 PK
 * @param remarks 更新後の備考
 */
public record RemarksPatchResponse(int id, String remarks) {}

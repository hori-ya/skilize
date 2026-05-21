package com.skilize.inventory.presentation.request;

import java.util.List;

/**
 * ITスキル明細一括保存リクエスト。PUT /api/inventories/{id}/it-skill-details のリクエストボディ。
 * 全件洗い替え方式のため、送信したリストがそのままDB上の明細として保存される。
 *
 * @param items ITスキル明細のリスト（空リスト送信で全件削除）
 */
public record ItSkillDetailsRequest(List<ItSkillDetailItem> items) {}

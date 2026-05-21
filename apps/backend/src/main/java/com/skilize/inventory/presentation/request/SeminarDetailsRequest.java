package com.skilize.inventory.presentation.request;

import java.util.List;

/**
 * セミナー明細一括保存リクエスト。PUT /api/inventories/{id}/seminar-details のリクエストボディ。
 * 全件洗い替え方式のため、送信したリストがそのままDB上の明細として保存される。
 *
 * @param items セミナー明細のリスト（空リスト送信で全件削除）
 */
public record SeminarDetailsRequest(List<SeminarDetailItem> items) {}

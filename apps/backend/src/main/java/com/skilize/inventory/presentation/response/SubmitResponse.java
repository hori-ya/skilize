package com.skilize.inventory.presentation.response;

/**
 * 棚卸提出レスポンス。POST /api/inventories/{id}/submit のレスポンスに使用する。
 * 提出後はステータスが PENDING_GOAL に遷移し、目標振り返りステップへ進む。
 *
 * @param id          提出した棚卸の内部 PK
 * @param status      提出後のステータス（PENDING_GOAL）
 * @param submittedAt 提出日時
 */
public record SubmitResponse(int id, String status, String submittedAt) {}

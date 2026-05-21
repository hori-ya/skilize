package com.skilize.master.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * ADセミナーマスタ登録・更新リクエスト。POST /api/ad-seminars および PUT /api/ad-seminars/{id} のリクエストボディ。
 *
 * @param categoryId  所属カテゴリID（任意。null の場合は未分類）
 * @param name        セミナー名（必須）
 * @param description 説明（任意）
 * @param sortOrder   表示順（任意）
 * @param active      有効フラグ（false で論理無効化）
 */
public record AdSeminarRequest(
        Integer categoryId,
        @NotBlank String name,
        String description,
        Integer sortOrder,
        Boolean active
) {}

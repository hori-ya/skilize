package com.skilize.master.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 汎用カテゴリ登録・更新リクエスト。資格カテゴリ・ADセミナーカテゴリ・セミナーカテゴリ等で共用する。
 * POST /api/{category-type} および PUT /api/{category-type}/{id} のリクエストボディ。
 *
 * @param name      カテゴリ名（必須）
 * @param sortOrder 表示順（任意）
 * @param active    有効フラグ（false で論理無効化）
 */
public record SimpleCategoryRequest(
        @NotBlank String name,
        Integer sortOrder,
        Boolean active
) {}

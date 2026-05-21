package com.skilize.master.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * ITスキルカテゴリ更新リクエスト。PUT /api/it-skill-categories/{id} のリクエストボディ。
 * 新規登録と異なり parentId は変更不可のため含まない。
 *
 * @param name      カテゴリ名（必須）
 * @param sortOrder 表示順（任意）
 * @param active    有効フラグ（false で論理無効化）
 */
public record ItSkillCategoryUpdateRequest(
        @NotBlank String name,
        Integer sortOrder,
        Boolean active
) {}

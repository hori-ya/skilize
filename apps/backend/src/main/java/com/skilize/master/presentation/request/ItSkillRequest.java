package com.skilize.master.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * ITスキルマスタ登録・更新リクエスト。POST /api/it-skills および PUT /api/it-skills/{id} のリクエストボディ。
 *
 * @param categoryId  所属カテゴリID（必須）
 * @param name        スキル名（必須）
 * @param description 説明（任意）
 * @param sortOrder   表示順（任意）
 * @param active      有効フラグ（false で論理無効化）
 */
public record ItSkillRequest(
        @NotNull Integer categoryId,
        @NotBlank String name,
        String description,
        Integer sortOrder,
        Boolean active
) {}

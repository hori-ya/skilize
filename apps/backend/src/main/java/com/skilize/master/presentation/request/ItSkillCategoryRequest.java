package com.skilize.master.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * ITスキルカテゴリ新規登録リクエスト。POST /api/it-skill-categories のリクエストボディ。
 * カテゴリは最大3階層（parentId=null でルート）。
 *
 * @param parentId  親カテゴリID（null の場合はルートカテゴリとして登録）
 * @param name      カテゴリ名（必須）
 * @param sortOrder 表示順（任意）
 */
public record ItSkillCategoryRequest(
        Integer parentId,
        @NotBlank String name,
        Integer sortOrder
) {}

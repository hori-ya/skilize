package com.skilize.master.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * カスタムITスキルのマスタ昇格リクエスト。POST /api/it-skills/promote のリクエストボディ。
 * ユーザーが自由入力したカスタムスキル名を正式マスタとして登録する。
 *
 * @param customName  昇格元のカスタムスキル名（既存の棚卸明細に保存されている名称と一致する必要あり）
 * @param categoryId  マスタ登録先カテゴリID（必須）
 * @param name        マスタ登録名（必須。customName と異なる名称を付けることも可）
 * @param description 説明（任意）
 * @param sortOrder   表示順（任意）
 */
public record PromoteItSkillRequest(
        @NotBlank String customName,
        @NotNull Integer categoryId,
        @NotBlank String name,
        String description,
        Integer sortOrder
) {}

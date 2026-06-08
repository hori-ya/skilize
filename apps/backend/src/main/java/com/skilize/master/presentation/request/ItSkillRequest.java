/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキルマスタ登録・更新リクエスト。カテゴリID・スキル名・説明・表示順・有効フラグを受け取る。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
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

/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格マスタ登録・更新リクエスト。カテゴリID・資格名・説明・表示順・有効フラグを受け取る。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.presentation.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 資格マスタ登録・更新リクエスト。POST /api/qualifications および PUT /api/qualifications/{id} のリクエストボディ。
 *
 * @param categoryId  所属カテゴリID（任意。null の場合は未分類）
 * @param name        資格名（必須）
 * @param description 説明（任意）
 * @param sortOrder   表示順（任意）
 * @param active      有効フラグ（false で論理無効化）
 */
public record QualificationRequest(
        Integer categoryId,
        @NotBlank String name,
        String description,
        Integer sortOrder,
        Boolean active
) {}

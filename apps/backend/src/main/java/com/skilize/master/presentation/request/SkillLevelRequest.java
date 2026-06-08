/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキルレベルマスタ登録・更新リクエスト。レベル値・説明・スコア重みを受け取る。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * スキルレベル登録・更新リクエスト。POST /api/skill-levels および PUT /api/skill-levels/{id} のリクエストボディ。
 *
 * @param levelValue  レベル値（1以上）
 * @param description レベルの説明
 * @param active      有効フラグ（false で論理無効化）
 * @param scoreWeight スコア集計時の重み（0以上）
 */
public record SkillLevelRequest(
        @NotNull @Min(1) Short levelValue,
        @NotBlank String description,
        Boolean active,
        @NotNull @Min(0) Integer scoreWeight
) {}

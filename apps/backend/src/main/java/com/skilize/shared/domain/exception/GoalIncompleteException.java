/**************************************************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 目標設定の件数バリデーション違反を表す例外クラス。
 * ITスキル/資格 ≥1 件・AD ≥2 件の条件違反時にスローし、422 Unprocessable Entity として返す。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.shared.domain.exception;

import java.util.List;

/**
 * 目標設定の件数バリデーション違反例外。
 * ITスキル/資格 ≥1 件・AD ≥2 件の条件を満たさずに目標完了操作を行った場合にスローされ、
 * GlobalExceptionHandler が 422 Unprocessable Entity として返す。
 */
public class GoalIncompleteException extends RuntimeException {

    private final List<GoalValidationError> errors;

    /**
     * 目標未完了例外を生成する。
     * @param errors バリデーション違反フィールドとメッセージのリスト
     */
    public GoalIncompleteException(List<GoalValidationError> errors) {
        super("GOAL_INCOMPLETE");
        this.errors = errors;
    }

    /**
     * バリデーション違反の詳細リストを返す。
     * @return フィールド名とエラーメッセージのペアのリスト
     */
    public List<GoalValidationError> getErrors() {
        return errors;
    }

    public record GoalValidationError(String field, String message) {}
}

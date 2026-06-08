/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * Excelファイルの形式不正を示す非検査例外。
 * MasterExcelControllerで400エラーに変換される。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.excel;

/** Excel ファイルの形式不正を示す非検査例外。Controller で 400 EXCEL_FORMAT_ERROR に変換する。 */
public class ExcelFormatException extends RuntimeException {

    /**
     * エラーメッセージ（エラーコード文字列）を指定して例外を生成する。
     *
     * @param message エラーコード文字列（例: "EXCEL_INVALID_FORMAT"）
     */
    public ExcelFormatException(String message) {
        super(message);
    }

    /**
     * エラーメッセージと原因例外を指定して例外を生成する。
     *
     * @param message エラーコード文字列
     * @param cause   原因例外
     */
    public ExcelFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * Excelセルスタイル生成ユーティリティ。マスタExcelエクスポーター共通のスタイル（ヘッダー・参考列・通常列）を提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Excel セルスタイル生成ユーティリティ。ヘッダー行・参考列のスタイルを提供する。 */
class ExcelStyleHelper {

    // ヘッダー背景: #D0E4F7
    private static final byte[] HEADER_RGB = {(byte) 0xD0, (byte) 0xE4, (byte) 0xF7};
    // 参考列背景: #F0F0F0
    private static final byte[] REF_RGB = {(byte) 0xF0, (byte) 0xF0, (byte) 0xF0};

    static XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(HEADER_RGB, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    static XSSFCellStyle createRefStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(REF_RGB, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    static XSSFCellStyle createNormalStyle(XSSFWorkbook wb) {
        return wb.createCellStyle();
    }

    private ExcelStyleHelper() {}
}

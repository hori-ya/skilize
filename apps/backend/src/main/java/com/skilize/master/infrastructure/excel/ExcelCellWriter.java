/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * Excelセル書き込みユーティリティ。全エクスポーターから共有するセル値設定・スタイル適用メソッドを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

/** Excel セル書き込みユーティリティ。全エクスポーターから共有する。 */
public final class ExcelCellWriter {

    private ExcelCellWriter() {}

    /**
     * 指定行・列に整数値を書き込む。
     *
     * @param row   対象行
     * @param col   列インデックス（0始まり）
     * @param value 書き込む整数値
     * @param style 適用するセルスタイル
     */
    public static void setInt(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col, CellType.NUMERIC);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * 指定行・列に整数値を書き込む。値が null の場合は空白セルとして書き込む。
     *
     * @param row   対象行
     * @param col   列インデックス（0始まり）
     * @param value 書き込む整数値（null の場合は空白）
     * @param style 適用するセルスタイル
     */
    public static void setIntOrBlank(Row row, int col, Integer value, CellStyle style) {
        if (value == null) {
            Cell cell = row.createCell(col, CellType.BLANK);
            cell.setCellStyle(style);
        } else {
            setInt(row, col, value, style);
        }
    }

    /**
     * 指定行・列に文字列値を書き込む。value が null の場合は空文字として書き込む。
     *
     * @param row   対象行
     * @param col   列インデックス（0始まり）
     * @param value 書き込む文字列（null の場合は空文字）
     * @param style 適用するセルスタイル
     */
    public static void setString(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col, CellType.STRING);
        String cellValue = "";
        if (value != null) {
            cellValue = value;
        }
        cell.setCellValue(cellValue);
        cell.setCellStyle(style);
    }

    /**
     * 指定行・列にヘッダーテキストを書き込む。setString と同等だが意図を明確にするために分けている。
     *
     * @param row   対象行
     * @param col   列インデックス（0始まり）
     * @param value ヘッダーテキスト
     * @param style 適用するセルスタイル（通常はヘッダースタイル）
     */
    public static void setHeader(Row row, int col, String value, CellStyle style) {
        setString(row, col, value, style);
    }
}

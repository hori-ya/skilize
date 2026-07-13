/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * Excelセル読み取りユーティリティ。全インポーターから共有するセル値の型変換・空行判定メソッドを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

/** Excel セル読み取りユーティリティ。全インポーターから共有する。 */
public final class ExcelCellReader {

    private ExcelCellReader() {}

    /**
     * 指定行・列の整数値を取得する。
     *
     * @param row 対象行（null の場合は null を返す）
     * @param col 列インデックス（0始まり）
     * @return 整数値（空・変換不可の場合は null）
     */
    public static Integer getInt(Row row, int col) {
        if (row == null) {
            return getInt((Cell) null);
        }
        return getInt(row.getCell(col));
    }

    /**
     * セルの整数値を取得する。NUMERIC・STRING 型に対応し、空・変換不可の場合は null を返す。
     *
     * @param cell 対象セル（null の場合は null を返す）
     * @return 整数値（空・変換不可の場合は null）
     */
    public static Integer getInt(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                String s = cell.getStringCellValue().trim();
                if (s.isEmpty()) {
                    return null;
                }
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    /**
     * 指定行・列の文字列値を取得する。
     *
     * @param row 対象行（null の場合は空文字を返す）
     * @param col 列インデックス（0始まり）
     * @return 文字列値（トリム済み。null の場合は空文字）
     */
    public static String getString(Row row, int col) {
        if (row == null) {
            return getString((Cell) null);
        }
        return getString(row.getCell(col));
    }

    /**
     * セルの文字列値を取得する。STRING・NUMERIC・BOOLEAN 型に対応し、null・空の場合は空文字を返す。
     *
     * @param cell 対象セル（null の場合は空文字を返す）
     * @return 文字列値（トリム済み）
     */
    public static String getString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /** 有効列の読み取り。「有効」→ true / 「無効」→ false / 空白 → null（省略扱い）。 */
    public static Boolean getActive(Row row, int col) {
        String val = getString(row, col);
        if ("有効".equals(val)) return Boolean.TRUE;
        if ("無効".equals(val)) return Boolean.FALSE;
        return null;
    }

    /** 指定列数分のセルがすべて空のとき true を返す（空行スキップ用）。 */
    public static boolean isEmptyRow(Row row, int numCols) {
        if (row == null) return true;
        for (int i = 0; i < numCols; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !getString(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}

package com.skilize.master.infrastructure.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

/** Excel セル書き込みユーティリティ。全エクスポーターから共有する。 */
public final class ExcelCellWriter {

    private ExcelCellWriter() {}

    public static void setInt(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col, CellType.NUMERIC);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    public static void setIntOrBlank(Row row, int col, Integer value, CellStyle style) {
        if (value == null) {
            Cell cell = row.createCell(col, CellType.BLANK);
            cell.setCellStyle(style);
        } else {
            setInt(row, col, value, style);
        }
    }

    public static void setString(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col, CellType.STRING);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    public static void setHeader(Row row, int col, String value, CellStyle style) {
        setString(row, col, value, style);
    }
}

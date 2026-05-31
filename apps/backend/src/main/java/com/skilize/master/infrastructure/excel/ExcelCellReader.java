package com.skilize.master.infrastructure.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

/** Excel セル読み取りユーティリティ。全インポーターから共有する。 */
public final class ExcelCellReader {

    private ExcelCellReader() {}

    public static Integer getInt(Row row, int col) {
        return getInt(row == null ? null : row.getCell(col));
    }

    public static Integer getInt(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                String s = cell.getStringCellValue().trim();
                try { yield s.isEmpty() ? null : Integer.parseInt(s); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    public static String getString(Row row, int col) {
        return getString(row == null ? null : row.getCell(col));
    }

    public static String getString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
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

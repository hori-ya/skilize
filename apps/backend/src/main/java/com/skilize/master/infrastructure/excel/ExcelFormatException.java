package com.skilize.master.infrastructure.excel;

/** Excel ファイルの形式不正を示す非検査例外。Controller で 400 EXCEL_FORMAT_ERROR に変換する。 */
public class ExcelFormatException extends RuntimeException {
    public ExcelFormatException(String message) {
        super(message);
    }

    public ExcelFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

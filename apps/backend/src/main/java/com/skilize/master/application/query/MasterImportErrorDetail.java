package com.skilize.master.application.query;

/** Excel 取込時の行レベルバリデーションエラー詳細。 */
public record MasterImportErrorDetail(String sheet, int row, String column, String message) {}

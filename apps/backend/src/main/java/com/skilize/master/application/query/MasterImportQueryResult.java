package com.skilize.master.application.query;

import java.util.List;

/** Excel 取込処理の結果。エラーがある場合は errors に詳細が入る（all-or-nothing）。 */
public record MasterImportQueryResult(int created, int updated, int deleted,
                                      List<MasterImportErrorDetail> errors) {

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public static MasterImportQueryResult ofErrors(List<MasterImportErrorDetail> errors) {
        return new MasterImportQueryResult(0, 0, 0, errors);
    }

    public static MasterImportQueryResult ofSuccess(int created, int updated, int deleted) {
        return new MasterImportQueryResult(created, updated, deleted, List.of());
    }
}

package com.skilize.master.presentation.response;

import com.skilize.master.application.query.MasterImportQueryResult;

/** Excel 取込成功時のレスポンス。 */
public record MasterImportResponse(int created, int updated, int deleted) {

    public static MasterImportResponse from(MasterImportQueryResult result) {
        return new MasterImportResponse(result.created(), result.updated(), result.deleted());
    }
}

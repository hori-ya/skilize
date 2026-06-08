/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * マスタExcel取込成功時のレスポンス。作成・更新・削除件数をクライアントに返す。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.presentation.response;

import com.skilize.master.application.query.MasterImportQueryResult;

/** Excel 取込成功時のレスポンス。 */
public record MasterImportResponse(int created, int updated, int deleted) {

    public static MasterImportResponse from(MasterImportQueryResult result) {
        return new MasterImportResponse(result.created(), result.updated(), result.deleted());
    }
}

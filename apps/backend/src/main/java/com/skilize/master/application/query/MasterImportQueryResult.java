/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * マスタExcel取込処理のクエリ結果。作成・更新・削除件数とエラー詳細リストを保持する。
 * エラーが1件でもある場合は all-or-nothing でロールバックされる。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.application.query;

import java.util.List;

/** Excel 取込処理の結果。エラーがある場合は errors に詳細が入る（all-or-nothing）。 */
public record MasterImportQueryResult(int created, int updated, int deleted,
                                      List<MasterImportErrorDetail> errors) {

    /**
     * エラーが1件以上存在するか判定する。
     *
     * @return エラーが存在する場合 true
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * エラーあり結果を生成するファクトリメソッド。作成・更新・削除件数はすべて 0 で返す。
     *
     * @param errors バリデーションエラー詳細リスト
     * @return エラーありの取込結果
     */
    public static MasterImportQueryResult ofErrors(List<MasterImportErrorDetail> errors) {
        return new MasterImportQueryResult(0, 0, 0, errors);
    }

    /**
     * 取込成功結果を生成するファクトリメソッド。エラーリストは空で返す。
     *
     * @param created 新規作成件数
     * @param updated 更新件数
     * @param deleted 論理削除件数
     * @return 成功の取込結果
     */
    public static MasterImportQueryResult ofSuccess(int created, int updated, int deleted) {
        return new MasterImportQueryResult(created, updated, deleted, List.of());
    }
}

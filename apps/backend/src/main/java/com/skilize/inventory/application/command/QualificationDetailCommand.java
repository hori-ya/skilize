/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格明細1件の保存コマンド。プレゼンテーション層からアプリケーション層へ資格明細データを伝達する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.application.command;

/**
 * 資格明細1件の保存コマンド。InventoryService.saveQualificationDetails() に渡す。
 * 全件洗い替え方式のため id はサーバー側では参照されない。
 *
 * @param id                      既存明細の内部 PK（全件洗い替えのためサーバー側では未使用）
 * @param qualificationId         資格マスタの ID（null の場合はカスタム資格）
 * @param customQualificationName カスタム資格名（qualificationId が null の場合のみ有効）
 * @param acquiredYearMonth       取得年月（ISO-8601 形式: "yyyy-MM-dd"。未設定の場合は null）
 * @param remarks                 備考（自由記述）
 */
public record QualificationDetailCommand(Integer id, Integer qualificationId,
                                         String customQualificationName,
                                         String acquiredYearMonth, String remarks) {}

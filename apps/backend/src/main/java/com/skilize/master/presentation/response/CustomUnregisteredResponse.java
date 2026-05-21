package com.skilize.master.presentation.response;

/**
 * カスタム未登録マスタ1件のレスポンス。GET /api/it-skills/custom-unregistered および
 * GET /api/qualifications/custom-unregistered のレスポンスに使用する。
 * ユーザーが自由入力した名称のうち、まだ正式マスタ化されていない候補を返す。
 *
 * @param customName 自由入力されたカスタム名称
 * @param usageCount 棚卸明細での使用回数
 */
public record CustomUnregisteredResponse(String customName, long usageCount) {}

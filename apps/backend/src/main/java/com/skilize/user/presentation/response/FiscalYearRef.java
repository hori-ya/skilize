package com.skilize.user.presentation.response;

/**
 * 年度の参照情報。ユーザー系レスポンス内に埋め込まれる年度サマリ。
 *
 * @param id   年度内部PK
 * @param name 年度名（例: "2024年度"）
 */
public record FiscalYearRef(int id, String name) {}

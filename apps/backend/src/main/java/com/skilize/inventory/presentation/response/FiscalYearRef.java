package com.skilize.inventory.presentation.response;

/**
 * 年度の参照情報（ID と名称のみの軽量なネスト型）。棚卸サマリ・詳細レスポンスに埋め込んで使用する。
 *
 * @param id   年度の内部 PK
 * @param name 年度名（例: "2024年度"）
 */
public record FiscalYearRef(int id, String name) {}

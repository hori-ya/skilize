/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * セミナー分類マスタドメインモデル。自由入力セミナーを分類するカテゴリを管理する。
 * ADセミナー分類（AdSeminarCategory）とは別テーブル・別系統。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.SeminarCategoryEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.model;

import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * セミナー分類マスタ。自由入力セミナー（SeminarDetail.seminarName）を分類するカテゴリ。
 * ADセミナーの分類（AdSeminarCategory）とは別テーブル。JPA/Springに依存しない純粋なドメインモデル。
 */
@Getter
public class SeminarCategory {

    private Integer id;
    private String name;
    private Integer sortOrder;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * 永続化済みの状態からセミナー分類を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static SeminarCategory reconstruct(Integer id, String name, Integer sortOrder, boolean active,
                                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        SeminarCategory c = new SeminarCategory();
        c.id = id;
        c.name = name;
        c.sortOrder = sortOrder;
        c.active = active;
        c.createdAt = createdAt;
        c.updatedAt = updatedAt;
        return c;
    }
}

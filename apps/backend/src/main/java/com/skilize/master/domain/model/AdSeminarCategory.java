/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナー分類マスタドメインモデル。ADセミナーを分類するフラットなカテゴリ（階層なし）を管理する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.AdSeminarCategoryEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** ADセミナー分類マスタ。ADセミナーを分類するフラットなカテゴリ（階層なし）。JPA/Springに依存しない純粋なドメインモデル。 */
@Getter
@NoArgsConstructor
public class AdSeminarCategory {

    private Integer id;
    private String name;
    private Integer sortOrder;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AdSeminarCategory create(String name, int sortOrder) {
        AdSeminarCategory c = new AdSeminarCategory();
        c.name = name;
        c.sortOrder = sortOrder;
        c.active = true;
        return c;
    }

    /**
     * 永続化済みの状態からADセミナー分類を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static AdSeminarCategory reconstruct(Integer id, String name, Integer sortOrder, boolean active,
                                                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        AdSeminarCategory c = new AdSeminarCategory();
        c.id = id;
        c.name = name;
        c.sortOrder = sortOrder;
        c.active = active;
        c.createdAt = createdAt;
        c.updatedAt = updatedAt;
        return c;
    }

    public void update(String name, int sortOrder, boolean active) {
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

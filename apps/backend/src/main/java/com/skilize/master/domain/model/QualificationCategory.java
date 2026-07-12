/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格分類マスタドメインモデル。資格を分類するフラットなカテゴリ（階層なし）を管理する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.QualificationCategoryEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** 資格分類マスタ。資格を分類するフラットなカテゴリ（階層なし）。JPA/Springに依存しない純粋なドメインモデル。 */
@Getter
@NoArgsConstructor
public class QualificationCategory {

    private Integer id;
    private String name;
    private Integer sortOrder;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static QualificationCategory create(String name, int sortOrder) {
        QualificationCategory c = new QualificationCategory();
        c.name = name;
        c.sortOrder = sortOrder;
        c.active = true;
        return c;
    }

    /**
     * 永続化済みの状態から資格分類を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static QualificationCategory reconstruct(Integer id, String name, Integer sortOrder, boolean active,
                                                     OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        QualificationCategory c = new QualificationCategory();
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

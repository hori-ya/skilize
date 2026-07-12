/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナーマスタドメインモデル。会社が推奨するADセミナーの一覧を管理する。
 * 棚卸セミナー明細および目標設定から参照される。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.AdSeminarEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ADセミナーマスタ。会社が推奨するADセミナーの一覧を管理する。JPA/Springに依存しない純粋なドメインモデル。
 * 棚卸セミナー明細（SeminarDetail）・目標設定（InventoryGoal）の両方から参照される。
 */
@Getter
@NoArgsConstructor
public class AdSeminar {

    private Integer id;
    private AdSeminarCategory category;
    private String name;
    private String description;
    private Integer sortOrder;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AdSeminar create(AdSeminarCategory category, String name, String description, int sortOrder) {
        AdSeminar a = new AdSeminar();
        a.category = category;
        a.name = name;
        a.description = description;
        a.sortOrder = sortOrder;
        a.active = true;
        return a;
    }

    /**
     * 永続化済みの状態からADセミナーを復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static AdSeminar reconstruct(Integer id, AdSeminarCategory category, String name, String description,
                                        Integer sortOrder, boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        AdSeminar a = new AdSeminar();
        a.id = id;
        a.category = category;
        a.name = name;
        a.description = description;
        a.sortOrder = sortOrder;
        a.active = active;
        a.createdAt = createdAt;
        a.updatedAt = updatedAt;
        return a;
    }

    public void update(AdSeminarCategory category, String name, String description, int sortOrder, boolean active) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

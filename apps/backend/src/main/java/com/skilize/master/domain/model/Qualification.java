/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格マスタドメインモデル。棚卸で取得状況を記録する資格の一覧を管理する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.QualificationEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 資格マスタ。棚卸で取得状況を記録する資格の一覧を管理する。JPA/Springに依存しない純粋なドメインモデル。
 * 無効化（is_active=false）は論理削除扱い。過去棚卸の明細参照を保持するため物理削除しない。
 */
@Getter
@NoArgsConstructor
public class Qualification {

    private Integer id;
    private QualificationCategory category;
    private String name;
    private String description;
    private Integer sortOrder;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static Qualification create(QualificationCategory category, String name, String description, int sortOrder) {
        Qualification q = new Qualification();
        q.category = category;
        q.name = name;
        q.description = description;
        q.sortOrder = sortOrder;
        q.active = true;
        return q;
    }

    /**
     * 永続化済みの状態から資格を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static Qualification reconstruct(Integer id, QualificationCategory category, String name, String description,
                                            Integer sortOrder, boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        Qualification q = new Qualification();
        q.id = id;
        q.category = category;
        q.name = name;
        q.description = description;
        q.sortOrder = sortOrder;
        q.active = active;
        q.createdAt = createdAt;
        q.updatedAt = updatedAt;
        return q;
    }

    public void update(QualificationCategory category, String name, String description, int sortOrder, boolean active) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

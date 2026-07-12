/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキルマスタドメインモデル。棚卸で採点対象となるITスキルを管理する。
 * カスタムスキル名のマスタ昇格にも使用される。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.ItSkillEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ITスキルマスタ。棚卸で採点対象となるITスキルの一覧を管理する。JPA/Springに依存しない純粋なドメインモデル。
 * 無効化（is_active=false）は論理削除扱い。過去棚卸の明細参照を保持するため物理削除しない。
 *
 * 項目（論理名）:
 *   分類         - 所属するITスキル分類（最大3階層の ItSkillCategory）
 *   スキル名     - スキルの表示名
 *   説明         - スキルの補足説明（任意）
 *   表示順       - 一覧表示時の並び順
 *   有効フラグ   - false で一覧から除外（論理削除）
 */
@Getter
@NoArgsConstructor
public class ItSkill {

    private Integer id;
    private ItSkillCategory category;
    private String name;
    private String description;
    private Integer sortOrder;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * ITスキルを新規生成する（有効状態で初期化）。
     */
    public static ItSkill create(ItSkillCategory category, String name, String description, int sortOrder) {
        ItSkill s = new ItSkill();
        s.category = category;
        s.name = name;
        s.description = description;
        s.sortOrder = sortOrder;
        s.active = true;
        return s;
    }

    /**
     * 永続化済みの状態からITスキルを復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static ItSkill reconstruct(Integer id, ItSkillCategory category, String name, String description,
                                      Integer sortOrder, boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        ItSkill s = new ItSkill();
        s.id = id;
        s.category = category;
        s.name = name;
        s.description = description;
        s.sortOrder = sortOrder;
        s.active = active;
        s.createdAt = createdAt;
        s.updatedAt = updatedAt;
        return s;
    }

    /**
     * ITスキルの各フィールドを更新する。
     */
    public void update(ItSkillCategory category, String name, String description, int sortOrder, boolean active) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

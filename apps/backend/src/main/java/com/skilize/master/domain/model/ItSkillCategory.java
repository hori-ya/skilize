/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル分類マスタドメインモデル。最大3階層の自己参照構造でITスキルを分類する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: infrastructure.persistence.entity.ItSkillCategoryEntity から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ITスキル分類マスタ。ITスキルを階層的に分類する。最大3階層まで対応（level=1〜3）。
 * JPA/Springに依存しない純粋なドメインモデル。親子関係は parentId（自己参照）で管理する。
 *
 * 項目（論理名）:
 *   親分類ID     - 親カテゴリの内部ID。null はルートカテゴリ（レベル1）
 *   親分類       - 親カテゴリ（読み取り専用。Mapperが再帰的に解決する）
 *   階層レベル   - 1=大分類 / 2=中分類 / 3=小分類
 *   分類名       - 分類の表示名
 *   表示順       - 一覧表示時の並び順
 *   有効フラグ   - false で一覧から除外（論理削除）
 */
@Getter
@NoArgsConstructor
public class ItSkillCategory {

    private Integer id;
    private Integer parentId;
    private ItSkillCategory parent;
    private Short level;
    private String name;
    private Integer sortOrder;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * ITスキル分類を新規生成する（有効状態で初期化）。
     */
    public static ItSkillCategory create(Integer parentId, short level, String name, int sortOrder) {
        ItSkillCategory c = new ItSkillCategory();
        c.parentId = parentId;
        c.level = level;
        c.name = name;
        c.sortOrder = sortOrder;
        c.active = true;
        return c;
    }

    /**
     * 永続化済みの状態からITスキル分類を復元する。infrastructure層のMapperからのみ呼び出す。
     */
    public static ItSkillCategory reconstruct(Integer id, Integer parentId, ItSkillCategory parent, Short level,
                                              String name, Integer sortOrder, boolean active,
                                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        ItSkillCategory c = new ItSkillCategory();
        c.id = id;
        c.parentId = parentId;
        c.parent = parent;
        c.level = level;
        c.name = name;
        c.sortOrder = sortOrder;
        c.active = active;
        c.createdAt = createdAt;
        c.updatedAt = updatedAt;
        return c;
    }

    /**
     * ITスキル分類の名称・表示順・有効フラグを更新する。
     */
    public void update(String name, int sortOrder, boolean active) {
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = active;
    }
}

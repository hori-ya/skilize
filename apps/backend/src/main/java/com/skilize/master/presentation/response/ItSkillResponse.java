package com.skilize.master.presentation.response;

import com.skilize.master.domain.ItSkill;
import com.skilize.master.domain.ItSkillCategory;

/**
 * ITスキルマスタ1件のレスポンス。GET /api/it-skills などのレスポンスに使用する。
 * カテゴリは最大3階層を展開して返す。
 *
 * @param id               ITスキル内部PK
 * @param name             スキル名
 * @param categoryId       直属カテゴリID
 * @param category1Id      第1階層（ルート）カテゴリID
 * @param category1Name    第1階層カテゴリ名
 * @param category1SortOrder 第1階層カテゴリ表示順
 * @param category2Name    第2階層カテゴリ名（存在しない場合は null）
 * @param category3Name    第3階層カテゴリ名（存在しない場合は null）
 * @param description      説明
 * @param sortOrder        表示順
 * @param isActive         有効フラグ
 */
public record ItSkillResponse(int id, String name, int categoryId, Integer category1Id, String category1Name,
                               int category1SortOrder,
                               String category2Name, String category3Name,
                               String description, int sortOrder, boolean isActive) {

    public static ItSkillResponse from(ItSkill s, ItSkillCategory cat1, ItSkillCategory cat2Category) {
        ItSkillCategory cat = s.getCategory();
        String cat2 = null, cat3 = null;
        if (cat.getLevel() == 3) {
            cat3 = cat.getName();
            cat2 = cat2Category != null ? cat2Category.getName() : null;
        } else if (cat.getLevel() == 2) {
            cat2 = cat.getName();
        }
        return new ItSkillResponse(s.getId(), s.getName(), cat.getId(),
                cat1 != null ? cat1.getId() : null,
                cat1 != null ? cat1.getName() : null,
                cat1 != null ? cat1.getSortOrder() : 0,
                cat2, cat3, s.getDescription(), s.getSortOrder(), s.isActive());
    }
}

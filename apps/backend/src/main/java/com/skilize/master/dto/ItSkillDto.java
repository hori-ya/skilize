package com.skilize.master.dto;

import com.skilize.master.domain.ItSkill;
import com.skilize.master.domain.ItSkillCategory;

public record ItSkillDto(int id, String name, int categoryId, Integer category1Id, String category1Name,
                          int category1SortOrder,
                          String category2Name, String category3Name,
                          String description, int sortOrder, boolean isActive) {

    public static ItSkillDto from(ItSkill s, ItSkillCategory cat1) {
        ItSkillCategory cat = s.getCategory();
        String cat2 = null, cat3 = null;
        if (cat.getLevel() == 3) {
            cat3 = cat.getName();
        } else if (cat.getLevel() == 2) {
            cat2 = cat.getName();
        }
        return new ItSkillDto(s.getId(), s.getName(), cat.getId(),
                cat1 != null ? cat1.getId() : null,
                cat1 != null ? cat1.getName() : null,
                cat1 != null ? cat1.getSortOrder() : 0,
                cat2, cat3, s.getDescription(), s.getSortOrder(), s.isActive());
    }
}

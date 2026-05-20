package com.skilize.master.dto;

import com.skilize.master.domain.ItSkillCategory;

public record ItSkillCategoryDto(int id, Integer parentId, short level, String name,
                                  int sortOrder, boolean isActive) {

    public static ItSkillCategoryDto from(ItSkillCategory c) {
        return new ItSkillCategoryDto(c.getId(), c.getParentId(), c.getLevel(),
                c.getName(), c.getSortOrder(), c.isActive());
    }
}

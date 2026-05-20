package com.skilize.master.dto;

import com.skilize.master.domain.SeminarCategory;

public record SeminarCategoryDto(int id, String name, int sortOrder, boolean isActive) {

    public static SeminarCategoryDto from(SeminarCategory c) {
        return new SeminarCategoryDto(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
    }
}

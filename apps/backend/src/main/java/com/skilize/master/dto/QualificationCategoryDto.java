package com.skilize.master.dto;

import com.skilize.master.domain.QualificationCategory;

public record QualificationCategoryDto(int id, String name, int sortOrder, boolean isActive) {

    public static QualificationCategoryDto from(QualificationCategory c) {
        return new QualificationCategoryDto(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
    }
}

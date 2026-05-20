package com.skilize.master.dto;

import com.skilize.master.domain.AdSeminarCategory;

public record AdSeminarCategoryDto(int id, String name, int sortOrder, boolean isActive) {

    public static AdSeminarCategoryDto from(AdSeminarCategory c) {
        return new AdSeminarCategoryDto(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
    }
}

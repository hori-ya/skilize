package com.skilize.master.dto;

import com.skilize.master.domain.AdSeminar;

public record AdSeminarDto(int id, String name, Integer categoryId, String categoryName,
                            String description, int sortOrder, boolean isActive) {

    public static AdSeminarDto from(AdSeminar a) {
        return new AdSeminarDto(a.getId(), a.getName(),
                a.getCategory() != null ? a.getCategory().getId() : null,
                a.getCategory() != null ? a.getCategory().getName() : null,
                a.getDescription(), a.getSortOrder(), a.isActive());
    }
}

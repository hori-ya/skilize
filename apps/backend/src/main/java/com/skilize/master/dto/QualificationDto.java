package com.skilize.master.dto;

import com.skilize.master.domain.Qualification;

public record QualificationDto(int id, String name, Integer categoryId, String categoryName,
                                String description, int sortOrder, boolean isActive) {

    public static QualificationDto from(Qualification q) {
        return new QualificationDto(q.getId(), q.getName(),
                q.getCategory() != null ? q.getCategory().getId() : null,
                q.getCategory() != null ? q.getCategory().getName() : null,
                q.getDescription(), q.getSortOrder(), q.isActive());
    }
}

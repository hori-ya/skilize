package com.skilize.inventory.dto;

import com.skilize.inventory.domain.SeminarDetail;

public record SeminarDetailDto(int id, Integer adSeminarId, String adSeminarName,
                                Integer adSeminarCategoryId, String adSeminarCategoryName,
                                String seminarName, Integer seminarCategoryId, String seminarCategoryName,
                                String attendedYearMonth, String remarks) {

    public static SeminarDetailDto from(SeminarDetail d) {
        return new SeminarDetailDto(d.getId(),
                d.getAdSeminar() != null ? d.getAdSeminar().getId() : null,
                d.getAdSeminar() != null ? d.getAdSeminar().getName() : null,
                d.getAdSeminar() != null && d.getAdSeminar().getCategory() != null ? d.getAdSeminar().getCategory().getId() : null,
                d.getAdSeminar() != null && d.getAdSeminar().getCategory() != null ? d.getAdSeminar().getCategory().getName() : null,
                d.getSeminarName(),
                d.getSeminarCategory() != null ? d.getSeminarCategory().getId() : null,
                d.getSeminarCategory() != null ? d.getSeminarCategory().getName() : null,
                d.getAttendedYearMonth() != null ? d.getAttendedYearMonth().toString() : null,
                d.getRemarks());
    }
}

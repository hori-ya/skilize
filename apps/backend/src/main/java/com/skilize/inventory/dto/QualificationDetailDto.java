package com.skilize.inventory.dto;

import com.skilize.inventory.domain.QualificationDetail;

public record QualificationDetailDto(int id, Integer qualificationId, String qualificationName,
                                      String qualificationCategoryName,
                                      String customQualificationName,
                                      String acquiredYearMonth, String remarks) {

    public static QualificationDetailDto from(QualificationDetail d) {
        return new QualificationDetailDto(d.getId(),
                d.getQualification() != null ? d.getQualification().getId() : null,
                d.getQualification() != null ? d.getQualification().getName() : null,
                d.getQualification() != null && d.getQualification().getCategory() != null
                        ? d.getQualification().getCategory().getName() : null,
                d.getCustomQualificationName(),
                d.getAcquiredYearMonth() != null ? d.getAcquiredYearMonth().toString() : null,
                d.getRemarks());
    }
}

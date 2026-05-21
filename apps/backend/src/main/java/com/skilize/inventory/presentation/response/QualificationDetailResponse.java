package com.skilize.inventory.presentation.response;

import com.skilize.inventory.domain.QualificationDetail;

/**
 * 資格明細1件のレスポンス。qualificationId が null の場合はカスタム資格（マスタ未登録）を示す。
 *
 * @param id                      明細の内部 PK
 * @param qualificationId         資格マスタの ID（カスタム資格の場合は null）
 * @param qualificationName       資格名（カスタム資格の場合は null）
 * @param qualificationCategoryName 資格分類名（分類未設定の場合は null）
 * @param customQualificationName カスタム資格名（マスタ登録資格の場合は null）
 * @param acquiredYearMonth       取得年月（未設定の場合は null）
 * @param remarks                 備考
 */
public record QualificationDetailResponse(int id, Integer qualificationId, String qualificationName,
                                          String qualificationCategoryName,
                                          String customQualificationName,
                                          String acquiredYearMonth, String remarks) {

    public static QualificationDetailResponse from(QualificationDetail d) {
        return new QualificationDetailResponse(d.getId(),
                d.getQualification() != null ? d.getQualification().getId() : null,
                d.getQualification() != null ? d.getQualification().getName() : null,
                d.getQualification() != null && d.getQualification().getCategory() != null
                        ? d.getQualification().getCategory().getName() : null,
                d.getCustomQualificationName(),
                d.getAcquiredYearMonth() != null ? d.getAcquiredYearMonth().toString() : null,
                d.getRemarks());
    }
}

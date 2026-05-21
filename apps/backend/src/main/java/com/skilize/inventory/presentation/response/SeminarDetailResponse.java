package com.skilize.inventory.presentation.response;

import com.skilize.inventory.domain.SeminarDetail;

/**
 * セミナー明細1件のレスポンス。ADセミナーと自由入力セミナーの両方を表現する。
 * adSeminarId が null の場合は自由入力セミナーで、seminarName / seminarCategoryId/Name が有効。
 *
 * @param id                  明細の内部 PK
 * @param adSeminarId         ADセミナーマスタの ID（自由入力セミナーの場合は null）
 * @param adSeminarName       ADセミナー名（自由入力セミナーの場合は null）
 * @param adSeminarCategoryId ADセミナー分類の ID（自由入力セミナーの場合は null）
 * @param adSeminarCategoryName ADセミナー分類名（自由入力セミナーの場合は null）
 * @param seminarName         セミナー名（自由入力の場合のみ値あり）
 * @param seminarCategoryId   セミナー分類 ID（自由入力の場合のみ値あり）
 * @param seminarCategoryName セミナー分類名（自由入力の場合のみ値あり）
 * @param attendedYearMonth   受講年月（未設定の場合は null）
 * @param remarks             備考
 */
public record SeminarDetailResponse(int id, Integer adSeminarId, String adSeminarName,
                                    Integer adSeminarCategoryId, String adSeminarCategoryName,
                                    String seminarName, Integer seminarCategoryId, String seminarCategoryName,
                                    String attendedYearMonth, String remarks) {

    public static SeminarDetailResponse from(SeminarDetail d) {
        return new SeminarDetailResponse(d.getId(),
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

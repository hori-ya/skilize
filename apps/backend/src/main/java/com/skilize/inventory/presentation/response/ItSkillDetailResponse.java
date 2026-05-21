package com.skilize.inventory.presentation.response;

import com.skilize.inventory.domain.ItSkillDetail;

/**
 * ITスキル明細1件のレスポンス。itSkillId が null の場合はカスタムスキル（マスタ未登録）を示す。
 *
 * @param id              明細の内部 PK
 * @param itSkillId       ITスキルマスタの ID（カスタムスキルの場合は null）
 * @param itSkillName     スキル名（カスタムスキルの場合は null）
 * @param customSkillName カスタムスキル名（マスタ登録スキルの場合は null）
 * @param skillLevelId    スキルレベルマスタの ID
 * @param levelValue      スキルレベル値（数値）
 * @param remarks         備考
 */
public record ItSkillDetailResponse(int id, Integer itSkillId, String itSkillName,
                                    String customSkillName, int skillLevelId,
                                    short levelValue, String remarks) {

    public static ItSkillDetailResponse from(ItSkillDetail d) {
        return new ItSkillDetailResponse(d.getId(),
                d.getItSkill() != null ? d.getItSkill().getId() : null,
                d.getItSkill() != null ? d.getItSkill().getName() : null,
                d.getCustomSkillName(),
                d.getSkillLevel().getId(), d.getSkillLevel().getLevelValue(), d.getRemarks());
    }
}

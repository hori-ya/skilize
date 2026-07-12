/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキル明細1件のレスポンス。ITスキル明細一覧取得・保存の各エンドポイントのレスポンス要素として使用する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.presentation.response;

import com.skilize.inventory.domain.model.ItSkillDetail;

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

    /**
     * ItSkillDetail エンティティから ItSkillDetailResponse を生成する。
     *
     * @param d ITスキル明細エンティティ
     * @return ITスキル明細レスポンス
     */
    public static ItSkillDetailResponse from(ItSkillDetail d) {
        return new ItSkillDetailResponse(d.getId(),
                d.getItSkill() != null ? d.getItSkill().getId() : null,
                d.getItSkill() != null ? d.getItSkill().getName() : null,
                d.getCustomSkillName(),
                d.getSkillLevel().getId(), d.getSkillLevel().getLevelValue(), d.getRemarks());
    }
}

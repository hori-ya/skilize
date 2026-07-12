/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * InventoryInterviewEntity（永続化モデル）と InventoryInterview（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.infrastructure.persistence.mapper;

import com.skilize.interview.domain.model.InventoryInterview;
import com.skilize.interview.infrastructure.persistence.entity.InventoryInterviewEntity;
import com.skilize.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** InventoryInterviewEntity ⇄ InventoryInterview の変換を担うMapper。interviewer は UserPersistenceMapper に委譲する。 */
@Component
@RequiredArgsConstructor
public class InventoryInterviewPersistenceMapper {

    private final UserPersistenceMapper userMapper;

    /** JPAエンティティからドメインモデルへ変換する。 */
    public InventoryInterview toDomain(InventoryInterviewEntity entity) {
        if (entity == null) return null;
        return InventoryInterview.reconstruct(entity.getId(), entity.getInventory().getId(),
                userMapper.toDomain(entity.getInterviewer()), entity.getGeneralNote(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

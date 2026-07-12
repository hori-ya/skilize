/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * InterviewDetailNoteEntity（永続化モデル）と InterviewDetailNote（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.infrastructure.persistence.mapper;

import com.skilize.interview.domain.model.InterviewDetailNote;
import com.skilize.interview.infrastructure.persistence.entity.InterviewDetailNoteEntity;
import org.springframework.stereotype.Component;

/** InterviewDetailNoteEntity ⇄ InterviewDetailNote の変換を担うMapper。 */
@Component
public class InterviewDetailNotePersistenceMapper {

    /** JPAエンティティからドメインモデルへ変換する。 */
    public InterviewDetailNote toDomain(InterviewDetailNoteEntity entity) {
        if (entity == null) return null;
        return InterviewDetailNote.reconstruct(entity.getId(), entity.getInterview().getId(),
                entity.getDetailType(), entity.getDetailId(), entity.getNote(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

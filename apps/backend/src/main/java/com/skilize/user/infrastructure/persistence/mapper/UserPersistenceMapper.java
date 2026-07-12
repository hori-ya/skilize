/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * UserEntity（永続化モデル）と User（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.infrastructure.persistence.mapper;

import com.skilize.user.domain.model.User;
import com.skilize.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

/** UserEntity ⇄ User の変換を担うMapper。 */
@Component
public class UserPersistenceMapper {

    /** JPAエンティティからドメインモデルへ変換する。 */
    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.reconstruct(entity.getId(), entity.getUserId(), entity.getName(), entity.getEmail(),
                entity.getPasswordHash(), entity.getRole(), entity.getTlUserId(), entity.isInitialPassword(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

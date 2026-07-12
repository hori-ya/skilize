/**************************************************************************************************************
 * 機能ID      ：EXP
 * 機能名      ：期待コメント
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * UserExpectationEntity（永続化モデル）と UserExpectation（ドメインモデル）を相互変換するMapper。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.expectation.infrastructure.persistence.mapper;

import com.skilize.expectation.domain.model.UserExpectation;
import com.skilize.expectation.infrastructure.persistence.entity.UserExpectationEntity;
import com.skilize.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** UserExpectationEntity ⇄ UserExpectation の変換を担うMapper。user は UserPersistenceMapper に委譲する。 */
@Component
@RequiredArgsConstructor
public class UserExpectationPersistenceMapper {

    private final UserPersistenceMapper userMapper;

    /** JPAエンティティからドメインモデルへ変換する。 */
    public UserExpectation toDomain(UserExpectationEntity entity) {
        if (entity == null) return null;
        return UserExpectation.reconstruct(entity.getUserId(), userMapper.toDomain(entity.getUser()),
                entity.getTlExpectation(), entity.getCompanyExpectation(),
                entity.getTlUpdatedAt(), entity.getCompanyUpdatedAt());
    }
}

/**************************************************************************************************************
 * 機能ID      ：EXP
 * 機能名      ：期待コメント
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.UserExpectationRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.expectation.infrastructure.persistence.repository;

import com.skilize.expectation.domain.model.UserExpectation;
import com.skilize.expectation.domain.repository.UserExpectationRepository;
import com.skilize.expectation.infrastructure.persistence.entity.UserExpectationEntity;
import com.skilize.expectation.infrastructure.persistence.mapper.UserExpectationPersistenceMapper;
import com.skilize.user.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** domain.repository.UserExpectationRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class UserExpectationRepositoryImpl implements UserExpectationRepository {

    private final UserExpectationJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final UserExpectationPersistenceMapper mapper;

    @Override
    public Optional<UserExpectation> findByUserId(Integer userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public UserExpectation save(UserExpectation expectation) {
        UserExpectationEntity entity = jpaRepository.findByUserId(expectation.getUser().getId())
                .orElseGet(() -> UserExpectationEntity.create(
                        userJpaRepository.getReferenceById(expectation.getUser().getId())));
        entity.applyState(expectation.getTlExpectation(), expectation.getCompanyExpectation(),
                expectation.getTlUpdatedAt(), expectation.getCompanyUpdatedAt());
        return mapper.toDomain(jpaRepository.save(entity));
    }
}

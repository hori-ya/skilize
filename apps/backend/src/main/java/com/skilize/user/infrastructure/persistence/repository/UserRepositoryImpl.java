/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.UserRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.infrastructure.persistence.repository;

import com.skilize.user.domain.model.User;
import com.skilize.user.domain.repository.UserRepository;
import com.skilize.user.infrastructure.persistence.entity.UserEntity;
import com.skilize.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** domain.repository.UserRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public Optional<User> findById(Integer id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity;
        if (user.getId() == null) {
            entity = UserEntity.create(user.getUserId(), user.getName(), user.getEmail(), user.getRole(),
                    user.getTlUserId(), user.getPasswordHash());
        } else {
            entity = jpaRepository.findById(user.getId())
                    .orElseThrow(() -> new IllegalStateException("User not found: id=" + user.getId()));
            entity.update(user.getName(), user.getEmail(), user.getRole(), user.getTlUserId(), user.isActive());
            // update() はパスワード関連フィールドを変更しないため、changePassword/resetPassword由来の変更を反映する
            if (!entity.getPasswordHash().equals(user.getPasswordHash())) {
                if (user.isInitialPassword()) {
                    entity.resetPassword(user.getPasswordHash());
                } else {
                    entity.changePassword(user.getPasswordHash());
                }
            }
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<User> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public List<User> findAllByOrderByUserIdAsc() {
        return jpaRepository.findAllByOrderByUserIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<User> findByTlUserIdAndActiveTrue(int tlUserId) {
        return jpaRepository.findByTlUserIdAndActiveTrue(tlUserId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<User> findByActiveTrue() {
        return jpaRepository.findByActiveTrue().stream().map(mapper::toDomain).toList();
    }
}

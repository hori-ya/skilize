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

import java.util.ArrayList;
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
        Optional<UserEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public User save(User user) {
        UserEntity entity;
        if (user.getId() == null) {
            entity = UserEntity.create(user.getUserId(), user.getName(), user.getEmail(), user.getRole(),
                    user.getTlUserId(), user.getPasswordHash());
        } else {
            Optional<UserEntity> entityOptional = jpaRepository.findById(user.getId());
            if (entityOptional.isEmpty()) {
                throw new IllegalStateException("User not found: id=" + user.getId());
            }
            entity = entityOptional.get();
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
        Optional<UserEntity> entityOptional = jpaRepository.findByUserId(userId);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public List<User> findAllByOrderByUserIdAsc() {
        List<User> users = new ArrayList<>();
        for (UserEntity entity : jpaRepository.findAllByOrderByUserIdAsc()) {
            users.add(mapper.toDomain(entity));
        }
        return users;
    }

    @Override
    public List<User> findByTlUserIdAndActiveTrue(int tlUserId) {
        List<User> users = new ArrayList<>();
        for (UserEntity entity : jpaRepository.findByTlUserIdAndActiveTrue(tlUserId)) {
            users.add(mapper.toDomain(entity));
        }
        return users;
    }

    @Override
    public List<User> findByActiveTrue() {
        List<User> users = new ArrayList<>();
        for (UserEntity entity : jpaRepository.findByActiveTrue()) {
            users.add(mapper.toDomain(entity));
        }
        return users;
    }
}

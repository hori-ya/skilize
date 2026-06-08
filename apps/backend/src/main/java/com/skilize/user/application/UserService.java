/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザーの作成・更新・パスワードリセットのビジネスロジックを担うサービスクラス。
 * 主に ADMIN 向け操作を提供し、TL向けのチーム照会補助メソッドも含む。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.application;

import com.skilize.fiscalyear.domain.FiscalYear;
import com.skilize.fiscalyear.domain.FiscalYearRepository;
import com.skilize.inventory.domain.Inventory;
import com.skilize.inventory.domain.InventoryRepository;
import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import com.skilize.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * ユーザーの作成・更新・パスワードリセットのビジネスロジック。ADMIN 操作が中心。
 * 新規作成時の初期パスワードはユーザーIDと同一（ログイン後に変更を強制する）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FiscalYearRepository fiscalYearRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * ユーザーを新規作成する。初期パスワードはユーザーIDと同一（BCrypt でハッシュ化して保存）。
     * ログイン後に InitialPasswordFilter が変更を強制する。
     */
    @Transactional
    public User create(String userId, String name, String email, Role role, Integer tlUserId) {
        // ユーザーIDの重複チェック（users.user_id に UNIQUE 制約があるが、アプリ側でも事前確認する）
        if (userRepository.findByUserId(userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "USER_ID_ALREADY_EXISTS");
        }
        // 初期パスワード = ユーザーID（BCrypt でハッシュ化。平文では保存しない）
        User saved = userRepository.save(User.create(userId, name, email, role, tlUserId,
                passwordEncoder.encode(userId)));
        log.info("User created: userId={} role={}", userId, role);
        return saved;
    }

    /** ユーザー情報（氏名・メール・ロール・上長・有効フラグ）を更新する。 */
    @Transactional
    public User update(int id, String name, String email, Role role, Integer tlUserId, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.update(name, email, role, tlUserId, active);
        User saved = userRepository.save(user);
        log.info("User updated: id={} userId={} role={} active={}", id, user.getUserId(), role, active);
        return saved;
    }

    /**
     * パスワードをユーザーIDに戻す（リセット）。
     * リセット後は is_initial_password が true になり、次回ログイン時に変更が強制される。
     * 仮パスワード（= ユーザーID）を戻り値として返し、ADMIN が本人に伝える運用を想定する。
     */
    @Transactional
    public String resetPassword(int id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // 仮パスワードはユーザーIDと同一（ADMIN から本人に口頭・メール等で伝える）
        String tempPassword = user.getUserId();
        user.resetPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        log.info("Password reset: id={} userId={}", id, user.getUserId());
        return tempPassword;
    }

    /** 今日の日付を基準に現在の有効な年度を取得する。 */
    @Transactional(readOnly = true)
    public Optional<FiscalYear> findCurrentFiscalYear() {
        return fiscalYearRepository.findCurrent(LocalDate.now());
    }

    /** 指定ユーザーの指定年度の棚卸を取得する。存在しない場合は Optional.empty() を返す。 */
    @Transactional(readOnly = true)
    public Optional<Inventory> findCurrentInventory(int userId, int fiscalYearId) {
        return inventoryRepository.findByUserIdAndFiscalYearId(userId, fiscalYearId);
    }
}

/**************************************************************************************************************
 * 機能ID      ：EXP
 * 機能名      ：期待コメント
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 期待コメント機能のアプリケーションサービス。TL期待・会社期待の取得・保存ビジネスロジックを担う。
 * TL期待は担当TLのみ、会社期待はADMINのみ編集可とするアクセス制御を実装する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.expectation.application;

import com.skilize.expectation.application.query.ExpectationQueryResult;
import com.skilize.expectation.domain.model.UserExpectation;
import com.skilize.expectation.domain.repository.UserExpectationRepository;
import com.skilize.user.domain.model.Role;
import com.skilize.user.domain.model.User;
import com.skilize.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * ユーザーへの期待（TL期待・会社期待）の取得・保存ビジネスロジック。
 * TL期待はそのユーザーの担当TL（tl_user_id が一致する TL）のみ編集可。
 * 会社期待は ADMIN のみ編集可。
 */
@Service
@RequiredArgsConstructor
public class ExpectationService {

    private final UserExpectationRepository expectationRepository;
    private final UserRepository userRepository;

    /**
     * 指定ユーザーの期待情報を取得する。
     * ADMIN は全ユーザー参照可。TL は担当チームメンバーのみ参照可。GENERAL はアクセス不可。
     * レコードが存在しない場合は空のレスポンスを返す（エラーにはしない）。
     */
    @Transactional(readOnly = true)
    public ExpectationQueryResult getForUser(int targetUserId, User requester) {
        requireAccess(targetUserId, requester);
        // map() で Optional<UserExpectation> → Optional<ExpectationQueryResult> に変換し、
        // 存在しない場合は empty() を返す
        return expectationRepository.findByUserId(targetUserId)
                .map(ExpectationQueryResult::from)
                .orElse(ExpectationQueryResult.empty());
    }

    /**
     * TL期待コメントを保存する。担当TL（tl_user_id が一致する TL ロールのユーザー）のみ編集可。
     * レコードが存在しない場合は新規作成する（upsert）。
     */
    @Transactional
    public ExpectationQueryResult saveTlExpectation(int targetUserId, User requester, String expectation) {
        // ロールが TL でない場合は弾く
        if (requester.getRole() != Role.TL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EXPECTATION_TL_ONLY");
        }
        User target = findUser(targetUserId);
        // 担当TLチェック: target.getTlUserId() が requester.getId() と一致する場合のみ許可する
        if (!requester.getId().equals(target.getTlUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EXPECTATION_ASSIGNED_TL_ONLY");
        }
        // upsert: レコードがなければ新規作成し、あれば取得して更新する
        UserExpectation entity = expectationRepository.findByUserId(targetUserId)
                .orElseGet(() -> UserExpectation.create(target));
        entity.updateTlExpectation(expectation);
        return ExpectationQueryResult.from(expectationRepository.save(entity));
    }

    /**
     * 会社期待コメントを保存する。ADMIN ロールのみ操作可（担当TLの制約なし）。
     * レコードが存在しない場合は新規作成する（upsert）。
     */
    @Transactional
    public ExpectationQueryResult saveCompanyExpectation(int targetUserId, User requester, String expectation) {
        if (requester.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EXPECTATION_ADMIN_ONLY");
        }
        User target = findUser(targetUserId);
        UserExpectation entity = expectationRepository.findByUserId(targetUserId)
                .orElseGet(() -> UserExpectation.create(target));
        entity.updateCompanyExpectation(expectation);
        return ExpectationQueryResult.from(expectationRepository.save(entity));
    }

    /**
     * 期待情報への参照権限を確認する。
     * ADMIN → 全員OK / TL → 担当チームメンバーのみ / GENERAL → 不可
     */
    private void requireAccess(int targetUserId, User requester) {
        // ADMIN はすべてのユーザーにアクセス可
        if (requester.getRole() == Role.ADMIN) return;
        if (requester.getRole() == Role.TL) {
            User target = findUser(targetUserId);
            // TL は担当チームメンバー（tl_user_id が自分のID と一致するユーザー）のみ参照可
            if (!requester.getId().equals(target.getTlUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EXPECTATION_TEAM_MEMBER_ONLY");
            }
            return;
        }
        // GENERAL ロールはアクセス不可
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    /** ユーザーをIDで取得する。存在しない場合は 404 をスローする共通ヘルパー。 */
    private User findUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));
    }
}

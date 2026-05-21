package com.skilize.expectation.application;

import com.skilize.expectation.application.query.ExpectationQueryResult;
import com.skilize.expectation.domain.UserExpectation;
import com.skilize.expectation.domain.UserExpectationRepository;
import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import com.skilize.user.domain.UserRepository;
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "TLが期待することはユーザーの担当TLのみ編集可能です");
        }
        User target = findUser(targetUserId);
        // 担当TLチェック: target.getTlUserId() が requester.getId() と一致する場合のみ許可する
        if (!requester.getId().equals(target.getTlUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "担当TLのみTL期待コメントを編集できます");
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "会社期待の設定は管理者のみ可能です");
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
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "チームメンバーの期待情報のみアクセス可能です");
            }
            return;
        }
        // GENERAL ロールはアクセス不可
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "アクセス権限がありません");
    }

    /** ユーザーをIDで取得する。存在しない場合は 404 をスローする共通ヘルパー。 */
    private User findUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));
    }
}

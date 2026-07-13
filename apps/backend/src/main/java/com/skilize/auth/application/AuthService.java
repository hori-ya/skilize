/**************************************************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ログイン・パスワード変更・自情報取得のビジネスロジックを実装するサービスクラス。
 * ユーザー列挙攻撃対策として、ユーザー不在とパスワード不一致で同一エラーを返す。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.auth.application;

import com.skilize.auth.application.command.ChangePasswordCommand;
import com.skilize.auth.application.command.LoginCommand;
import com.skilize.auth.application.query.LoginQueryResult;
import com.skilize.auth.application.query.MeQueryResult;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.infrastructure.JwtUtil;
import com.skilize.user.domain.model.User;
import com.skilize.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * ログイン・パスワード変更・自情報取得のビジネスロジック。
 * ユーザーIDの存在有無を外部に漏らさないため、ユーザー不在とパスワード不一致で同一エラーを返す。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * ログイン処理。ユーザーID・パスワードを検証し、成功時に JWT を発行して返す。
     * ユーザー不在とパスワード不一致を同一エラーにすることで、ユーザーIDの存在有無を外部に漏らさない。
     */
    public LoginQueryResult login(LoginCommand command) {
        // ユーザーIDが存在しない場合もパスワード不一致と同じエラーメッセージを返す（ユーザー列挙攻撃対策）
        Optional<User> userOptional = userRepository.findByUserId(command.userId());
        if (userOptional.isEmpty()) {
            throw new AuthException("AUTH_FAILED", "");
        }
        User user = userOptional.get();

        // 無効化済みアカウントは認証前に弾く
        if (!user.isActive()) {
            throw new AuthException("ACCOUNT_DISABLED", "");
        }

        // passwordEncoder.matches(): 入力値をハッシュ化して DB のハッシュと比較する（BCrypt の遅い照合が実行される）
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new AuthException("AUTH_FAILED", "");
        }

        // 認証成功。JWT を生成してユーザー情報と合わせてクエリ結果を組み立てる
        return buildLoginQueryResult(jwtUtil.generateToken(user), user);
    }

    /**
     * パスワード変更処理。現在のパスワードを確認した上で新しいパスワードに更新する。
     * 変更完了後は is_initial_password が false になり、初回変更強制が解除される。
     */
    @Transactional
    public void changePassword(ChangePasswordCommand command, User currentUser) {
        // SecurityContext のユーザーは古い状態の可能性があるため ID で再フェッチしてから更新する
        Optional<User> userOptional = userRepository.findById(currentUser.getId());
        if (userOptional.isEmpty()) {
            throw new NoSuchElementException();
        }
        User user = userOptional.get();
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new AuthException("CURRENT_PASSWORD_WRONG", "");
        }
        // passwordEncoder.encode(): 新しいパスワードを BCrypt でハッシュ化してから保存する
        user.changePassword(passwordEncoder.encode(command.newPassword()));
        // User はJPA管理外の純粋なドメインモデルのため、明示的にsaveしない限りDBへ反映されない
        userRepository.save(user);
    }

    /**
     * 自分のユーザー情報を取得する。JWT から復元した認証済みユーザーをクエリ結果に変換して返す。
     */
    public MeQueryResult getMe(User user) {
        return new MeQueryResult(
                user.getId(),
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isInitialPassword(),
                resolveTlUser(user.getTlUserId()),
                user.isActive()
        );
    }

    /** ログイン成功時のクエリ結果オブジェクトを組み立てる。 */
    private LoginQueryResult buildLoginQueryResult(String token, User user) {
        LoginQueryResult.UserInfo userInfo = new LoginQueryResult.UserInfo(
                user.getId(),
                user.getName(),
                user.getRole().name(),
                user.isInitialPassword(),
                resolveTlUser(user.getTlUserId())
        );
        return new LoginQueryResult(token, userInfo);
    }

    /** TLユーザーIDが設定されている場合のみ DB を参照してTL情報（ID・氏名）を返す。null は上長なし。 */
    private LoginQueryResult.TlUserInfo resolveTlUser(Integer tlUserId) {
        if (tlUserId == null) {
            return null;
        }
        Optional<User> tlUserOptional = userRepository.findById(tlUserId);
        if (tlUserOptional.isEmpty()) {
            return null;
        }
        User tlUser = tlUserOptional.get();
        return new LoginQueryResult.TlUserInfo(tlUser.getId(), tlUser.getName());
    }
}

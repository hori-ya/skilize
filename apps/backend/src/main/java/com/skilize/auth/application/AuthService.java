package com.skilize.auth.application;

import com.skilize.auth.application.command.ChangePasswordCommand;
import com.skilize.auth.application.command.LoginCommand;
import com.skilize.auth.application.query.LoginQueryResult;
import com.skilize.auth.application.query.MeQueryResult;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.infrastructure.JwtUtil;
import com.skilize.user.domain.User;
import com.skilize.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        User user = userRepository.findByUserId(command.userId())
                .orElseThrow(() -> new AuthException("AUTH_FAILED", ""));

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
        // SecurityContext のユーザーは JPA 管理外の可能性があるため ID で再フェッチしてトランザクション内で更新する
        // （JPA の管理外エンティティへの変更は DB に保存されない）
        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new AuthException("CURRENT_PASSWORD_WRONG", "");
        }
        // passwordEncoder.encode(): 新しいパスワードを BCrypt でハッシュ化してから保存する
        user.changePassword(passwordEncoder.encode(command.newPassword()));
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
        if (tlUserId == null) return null;
        // map(): Optional の中身を変換する。TLが存在しない場合は orElse(null) で空を返す。
        return userRepository.findById(tlUserId)
                .map(tl -> new LoginQueryResult.TlUserInfo(tl.getId(), tl.getName()))
                .orElse(null);
    }
}

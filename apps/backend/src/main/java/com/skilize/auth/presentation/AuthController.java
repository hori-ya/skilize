package com.skilize.auth.presentation;

import com.skilize.auth.application.AuthService;
import com.skilize.auth.application.mapper.AuthApplicationMapper;
import com.skilize.auth.application.query.LoginQueryResult;
import com.skilize.auth.application.query.MeQueryResult;
import com.skilize.auth.presentation.request.ChangePasswordRequest;
import com.skilize.auth.presentation.request.LoginRequest;
import com.skilize.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 認証系 REST API コントローラー。
 * /login のみ認証不要。他のエンドポイントは JwtAuthenticationFilter で認証済みユーザーが注入される。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthApplicationMapper authApplicationMapper;

    /**
     * ログイン。ユーザーID・パスワードを検証し、JWT を返す。
     * SecurityConfig で permitAll() 設定済み（認証不要エンドポイント）。
     */
    @PostMapping("/login")
    public ResponseEntity<LoginQueryResult> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(authApplicationMapper.toCommand(request)));
    }

    /**
     * ログアウト。サーバー側の処理は不要（JWT はステートレスなのでサーバーに状態がない）。
     * クライアント側が localStorage の JWT を削除することでログアウトが完了する。
     * 204 No Content を返すだけの空実装。
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    /**
     * パスワード変更。現在のパスワードを確認した上で新しいパスワードに更新する。
     * 初回ログイン後の強制変更にも通常のパスワード変更にも使用する。
     * @param user @AuthenticationPrincipal → JwtAuthenticationFilter が SecurityContext に格納したユーザー
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               @AuthenticationPrincipal User user) {
        authService.changePassword(authApplicationMapper.toCommand(request), user);
        return ResponseEntity.noContent().build();
    }

    /**
     * 認証済みユーザーの情報を返す。アプリ起動時にフロントエンドが呼び出し、セッションを復元する。
     * @param user @AuthenticationPrincipal → JwtAuthenticationFilter が JWT から復元したユーザー
     */
    @GetMapping("/me")
    public ResponseEntity<MeQueryResult> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getMe(user));
    }
}

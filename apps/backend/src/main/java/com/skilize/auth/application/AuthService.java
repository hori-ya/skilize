package com.skilize.auth.application;

import com.skilize.auth.dto.ChangePasswordRequest;
import com.skilize.auth.dto.LoginRequest;
import com.skilize.auth.dto.LoginResponse;
import com.skilize.auth.dto.MeResponse;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.infrastructure.JwtUtil;
import com.skilize.user.domain.User;
import com.skilize.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUserId(request.userId())
                .orElseThrow(() -> new AuthException("AUTH_FAILED", "ユーザーIDまたはパスワードが違います"));

        if (!user.isActive()) {
            throw new AuthException("FORBIDDEN", "このアカウントは無効化されています");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("AUTH_FAILED", "ユーザーIDまたはパスワードが違います");
        }

        return buildLoginResponse(jwtUtil.generateToken(user), user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, User currentUser) {
        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthException("AUTH_FAILED", "現在のパスワードが正しくありません");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    public MeResponse getMe(User user) {
        return new MeResponse(
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

    private LoginResponse buildLoginResponse(String token, User user) {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getName(),
                user.getRole().name(),
                user.isInitialPassword(),
                resolveTlUser(user.getTlUserId())
        );
        return new LoginResponse(token, userInfo);
    }

    private LoginResponse.TlUserInfo resolveTlUser(Integer tlUserId) {
        if (tlUserId == null) return null;
        return userRepository.findById(tlUserId)
                .map(tl -> new LoginResponse.TlUserInfo(tl.getId(), tl.getName()))
                .orElse(null);
    }
}

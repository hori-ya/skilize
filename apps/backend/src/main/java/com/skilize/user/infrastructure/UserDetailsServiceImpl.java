package com.skilize.user.infrastructure;

import com.skilize.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security の UserDetailsService 実装。
 * Spring Security のフォームログイン・DaoAuthenticationProvider が認証時にこのクラスを呼び出し、
 * DB からユーザーを取得してパスワード検証を行う。
 * このプロジェクトでは JWT 認証を採用しているため、本クラスは直接のログイン認証では使われず、
 * JwtAuthenticationFilter から SecurityContext にユーザーをロードする際に間接的に使われる可能性がある。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * ユーザーID（userId）を使って DB からユーザーを取得する。
     * Spring Security の username は、このプロジェクトでは内部 PK ではなくユーザーID文字列（例: "user01"）。
     * ユーザーが見つからない場合は UsernameNotFoundException をスローする（Spring Security の規約）。
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUserId(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}

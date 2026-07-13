/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * Spring Security の UserDetailsService 実装クラス。
 * JwtAuthenticationFilter から SecurityContext にユーザーをロードする際に使用される。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: user/infrastructure/直下からsecurity/へ移動、UserPrincipalでラップして返すよう変更
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.infrastructure.security;

import com.skilize.user.domain.model.User;
import com.skilize.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        Optional<User> userOptional = userRepository.findByUserId(username);
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return new UserPrincipal(userOptional.get());
    }
}

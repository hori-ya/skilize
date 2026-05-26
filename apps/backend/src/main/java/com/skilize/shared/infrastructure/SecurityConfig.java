package com.skilize.shared.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security の全体設定クラス。
 * フィルターチェーン（JWT検証 → 初回PW変更強制）・CORS・BCrypt を一元管理する。
 * ロール別アクセス制御は @PreAuthorize でコントローラー側に実装し、ここでは URL 単位の認証のみ定義する。
 *
 * @EnableMethodSecurity: コントローラーメソッドに @PreAuthorize("hasRole('ADMIN')") 等を使えるようにする。
 *                         これを付けないと @PreAuthorize が無視される。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LoggingFilter loggingFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InitialPasswordFilter initialPasswordFilter;

    @Value("${app.frontend-origin}")
    private String frontendOrigin;

    // 環境変数 FRONTEND_ORIGIN はカンマ区切りで複数オリジンを指定できる
    private List<String> allowedOrigins() {
        return java.util.Arrays.stream(frontendOrigin.split(","))
                .map(String::trim)
                .toList();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API のため CSRF 不要。JWT でステートレス認証を行う。
                // CSRF はブラウザのクッキー自動送信を悪用する攻撃。JWT を Authorization ヘッダーで送る場合は対象外。
                .csrf(AbstractHttpConfigurer::disable)
                // STATELESS: Spring がHTTPセッションを一切生成・使用しない。認証情報はリクエストごとの JWT のみ。
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        // 上記以外はすべて認証必須。ロール別制御は各コントローラーの @PreAuthorize が担う。
                        .anyRequest().authenticated()
                )
                // フィルター順: LoggingFilter(MDC) → JwtAuthenticationFilter(認証) → InitialPasswordFilter(初回PW)
                // JwtAuthenticationFilter を先に登録してから、LoggingFilter を「その前」として参照する
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loggingFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(initialPasswordFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // コストファクター 12 = ログイン照合が約 300〜500ms になる強度（セキュリティ設計書参照）
        // コストを上げるほどブルートフォース耐性が上がるが、ログインの応答時間も増える
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // プリフライトキャッシュ: 1時間

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

package com.skilize.shared.infrastructure;

import com.skilize.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT の生成・検証ユーティリティ。署名アルゴリズムは HS256。
 * ペイロード: sub=ユーザー内部ID(integer)、name=氏名、role=ロール名。
 * ロールは JWT に含めるが、認可チェックは DB 上のロールを正とする（JWT のロール値を信頼しすぎない）。
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * ユーザー情報から JWT を生成する。
     * ペイロードに sub（内部ID）・name（氏名）・role（ロール名）を埋め込む。
     */
    public String generateToken(User user) {
        return Jwts.builder()
                // sub（subject）: JWT の主体を表す標準クレーム。ここでは DB の内部ID を文字列で格納する
                .subject(String.valueOf(user.getId()))
                // カスタムクレーム: フロントエンドでユーザー名・ロールを表示するために含める
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                // 有効期限: 環境変数 JWT_EXPIRATION_MS で設定（デフォルト8時間）
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                // HS256 で署名する。署名がないと改ざん検知ができない
                .signWith(getSigningKey())
                // JWT 文字列（Base64URL エンコードされた "header.payload.signature" 形式）に変換する
                .compact();
    }

    /**
     * JWT からユーザーの内部ID（sub クレーム）を文字列で取得する。
     * 取得した文字列を Integer.valueOf() で数値に変換して DB 検索に使う。
     */
    public String extractUserId(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * JWT の署名と有効期限を検証する。
     * 検証に失敗した場合（改ざん・期限切れ等）は JwtException が発生するため、例外をキャッチして false を返す。
     */
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException: 改ざん・期限切れ等
            // IllegalArgumentException: 空文字・null 等の不正な入力値
            return false;
        }
    }

    /**
     * JWT を解析してペイロード（Claims オブジェクト）を取得する。
     * 署名検証・有効期限チェックも同時に行い、問題があれば JwtException をスローする。
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 署名検証用の HMAC-SHA256 鍵を生成する。
     * 環境変数 JWT_SECRET の文字列バイト列から鍵を生成する（32文字以上を推奨）。
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

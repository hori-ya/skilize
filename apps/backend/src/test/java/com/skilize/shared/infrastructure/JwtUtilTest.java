package com.skilize.shared.infrastructure;

import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * JwtUtil の単体テスト。Spring コンテキスト不使用。@Value フィールドは ReflectionTestUtils で注入する。
 */
class JwtUtilTest {

    private static final String TEST_SECRET =
            "test-secret-key-for-testing-purposes-minimum-32chars!!";

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3_600_000L);

        testUser = User.create("user01", "テストユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(testUser, "id", 42);
    }

    // ═══════════════════════════════════════════════════════════
    //  generateToken
    // ═══════════════════════════════════════════════════════════

    @Test
    void generateToken_正常系_3パートのJWT文字列を返す() {
        String token = jwtUtil.generateToken(testUser);

        assertThat(token).isNotBlank();
        // JWT は header.payload.signature の3パート構造
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ═══════════════════════════════════════════════════════════
    //  extractUserId
    // ═══════════════════════════════════════════════════════════

    @Test
    void extractUserId_正常系_ユーザー内部IDを文字列で返す() {
        String token = jwtUtil.generateToken(testUser);

        assertThat(jwtUtil.extractUserId(token)).isEqualTo("42");
    }

    // ═══════════════════════════════════════════════════════════
    //  isTokenValid
    // ═══════════════════════════════════════════════════════════

    @Test
    void isTokenValid_有効なトークン_trueを返す() {
        String token = jwtUtil.generateToken(testUser);

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_不正な文字列_falseを返す() {
        assertThat(jwtUtil.isTokenValid("invalid.token.value")).isFalse();
    }

    @Test
    void isTokenValid_空文字_falseを返す() {
        assertThat(jwtUtil.isTokenValid("")).isFalse();
    }

    @Test
    void isTokenValid_期限切れトークン_falseを返す() {
        // expirationMs を負値にすることで、生成直後でも期限切れになるトークンを作る
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1L);
        String expiredToken = jwtUtil.generateToken(testUser);

        assertThat(jwtUtil.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void isTokenValid_異なる秘密鍵で検証_falseを返す() {
        String token = jwtUtil.generateToken(testUser);

        JwtUtil otherUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherUtil, "secret",
                "other-secret-key-for-testing-purposes-minimum-32chars!!");
        ReflectionTestUtils.setField(otherUtil, "expirationMs", 3_600_000L);

        assertThat(otherUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void generateAndExtract_ラウンドトリップ_整合性を確認() {
        User user1 = User.create("userA", "ユーザーA", null, Role.TL, null, "hash");
        ReflectionTestUtils.setField(user1, "id", 99);

        String token = jwtUtil.generateToken(user1);

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("99");
    }
}

-- 初期管理者ユーザーの初期パスワードフラグを解除
-- 本番運用前にパスワードを変更してください
UPDATE users SET is_initial_password = FALSE WHERE user_id = 'admin';

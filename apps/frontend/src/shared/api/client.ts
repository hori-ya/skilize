/**
 * Axios クライアント。全リクエストに localStorage の JWT を Authorization ヘッダーとして付与する。
 * /auth/login のみ認証前エンドポイントのため Authorization ヘッダーを付与しない。
 */
import axios from 'axios';

// axios.create(): ベースURL・デフォルトヘッダー等を共通設定した Axios インスタンスを生成する
// すべての API 呼び出しはこのインスタンスを経由することで設定が統一される
const apiClient = axios.create({
  // nginx が /api/** を Spring Boot（:8080）へプロキシするため、相対パスで指定する
  baseURL: '/api',
});

// interceptors.request.use(): 全リクエスト送信前に実行されるミドルウェア
// JWT を自動付与することで、各 API 呼び出しで手動設定する必要がなくなる
apiClient.interceptors.request.use((config) => {
  // /auth/login はトークン取得前のエンドポイントのため Authorization ヘッダーを付与しない
  if (config.url === '/auth/login') {
    return config;
  }
  const token = localStorage.getItem('authToken');
  if (token) {
    // Authorization: Bearer <token> 形式が HTTP の標準的な Bearer 認証スキーム
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default apiClient;

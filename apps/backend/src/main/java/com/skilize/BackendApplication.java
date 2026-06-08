/**************************************************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * Spring Boot アプリケーションのエントリーポイント。
 * @SpringBootApplication によるコンポーネントスキャンと自動設定を起動する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Skilize バックエンドアプリケーションのメインクラス。
 * Spring Boot を起動し、全コンポーネントの初期化を行う。
 * @EnableAsync により非同期処理（AI分析サービス）を有効化している。
 */
@SpringBootApplication
// @EnableAsync: @Async アノテーションを有効化する。AI分析の非同期実行（AiAnalysisService）に必要。
@EnableAsync
public class BackendApplication {

	/**
	 * アプリケーションのエントリーポイント。
	 * @param args コマンドライン引数（Spring Boot により環境変数として処理される）
	 */
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}

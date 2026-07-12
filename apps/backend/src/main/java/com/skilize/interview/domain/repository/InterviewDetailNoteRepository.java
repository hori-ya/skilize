/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談明細ノートリポジトリインターフェース。永続化の実装詳細を持たない。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: JpaRepositoryの直接継承をやめ、実装はinfrastructure層のInterviewDetailNoteRepositoryImplへ移動
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.domain.repository;

import com.skilize.interview.domain.model.InterviewDetailNote;

import java.util.List;

/** 面談明細ノートリポジトリ。ヘッダー（InventoryInterview）保存時に全件洗い替えで使用する。実装は infrastructure.persistence.repository.InterviewDetailNoteRepositoryImpl。 */
public interface InterviewDetailNoteRepository {

    /** 面談明細ノートを一括保存する。 */
    List<InterviewDetailNote> saveAll(List<InterviewDetailNote> notes);

    /** 指定面談メモヘッダーに紐づく全明細ノートを返す。 */
    List<InterviewDetailNote> findByInterviewId(int interviewId);

    /** 指定面談メモヘッダーの全明細ノートを削除する。全件洗い替え時の DELETE に使用する。 */
    void deleteByInterviewId(int interviewId);
}

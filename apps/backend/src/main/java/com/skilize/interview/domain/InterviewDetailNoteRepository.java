/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談明細ノートリポジトリ。interview_detail_notes テーブルへのデータアクセスを提供する。
 * 面談メモヘッダー保存時の全件洗い替え（削除・再INSERT）に使用するクエリを定義する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 面談明細ノートリポジトリ。ヘッダー（InventoryInterview）保存時に全件洗い替えで使用する。
 */
public interface InterviewDetailNoteRepository extends JpaRepository<InterviewDetailNote, Integer> {

    /** 指定面談メモヘッダーに紐づく全明細ノートを返す。 */
    List<InterviewDetailNote> findByInterviewId(int interviewId);

    /** 指定面談メモヘッダーの全明細ノートを削除する。全件洗い替え時の DELETE に使用する。 */
    @Modifying
    @Query("DELETE FROM InterviewDetailNote d WHERE d.interview.id = :interviewId")
    void deleteByInterviewId(@Param("interviewId") int interviewId);
}

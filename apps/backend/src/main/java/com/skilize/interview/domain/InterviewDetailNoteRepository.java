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

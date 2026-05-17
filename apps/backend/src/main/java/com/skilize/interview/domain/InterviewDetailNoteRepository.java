package com.skilize.interview.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewDetailNoteRepository extends JpaRepository<InterviewDetailNote, Integer> {

    List<InterviewDetailNote> findByInterviewId(int interviewId);

    @Modifying
    @Query("DELETE FROM InterviewDetailNote d WHERE d.interview.id = :interviewId")
    void deleteByInterviewId(@Param("interviewId") int interviewId);
}

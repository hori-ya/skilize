/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.InterviewDetailNoteRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.infrastructure.persistence.repository;

import com.skilize.interview.domain.model.InterviewDetailNote;
import com.skilize.interview.domain.repository.InterviewDetailNoteRepository;
import com.skilize.interview.infrastructure.persistence.entity.InterviewDetailNoteEntity;
import com.skilize.interview.infrastructure.persistence.mapper.InterviewDetailNotePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/** domain.repository.InterviewDetailNoteRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class InterviewDetailNoteRepositoryImpl implements InterviewDetailNoteRepository {

    private final InterviewDetailNoteJpaRepository jpaRepository;
    private final InventoryInterviewJpaRepository interviewJpaRepository;
    private final InterviewDetailNotePersistenceMapper mapper;

    @Override
    public List<InterviewDetailNote> saveAll(List<InterviewDetailNote> notes) {
        List<InterviewDetailNoteEntity> entities = new ArrayList<>();
        for (InterviewDetailNote note : notes) {
            entities.add(InterviewDetailNoteEntity.create(
                    interviewJpaRepository.getReferenceById(note.getInterviewId()),
                    note.getDetailType(), note.getDetailId(), note.getNote()));
        }
        List<InterviewDetailNote> saved = new ArrayList<>();
        for (InterviewDetailNoteEntity entity : jpaRepository.saveAll(entities)) {
            saved.add(mapper.toDomain(entity));
        }
        return saved;
    }

    @Override
    public List<InterviewDetailNote> findByInterviewId(int interviewId) {
        List<InterviewDetailNote> notes = new ArrayList<>();
        for (InterviewDetailNoteEntity entity : jpaRepository.findByInterviewId(interviewId)) {
            notes.add(mapper.toDomain(entity));
        }
        return notes;
    }

    @Override
    public void deleteByInterviewId(int interviewId) {
        jpaRepository.deleteByInterviewId(interviewId);
    }
}

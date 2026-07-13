/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 面談メモ機能のプレゼンテーション層（TL/ADMIN 向け）。棚卸IDをキーに面談メモの取得・保存・
 * 前年度参照エンドポイントを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.presentation;

import com.skilize.interview.application.InterviewService;
import com.skilize.interview.application.command.DetailNoteCommand;
import com.skilize.interview.domain.model.InterviewDetailNote;
import com.skilize.interview.domain.model.InventoryInterview;
import com.skilize.interview.presentation.request.DetailNoteRequest;
import com.skilize.interview.presentation.request.SaveInterviewRequest;
import com.skilize.interview.presentation.response.DetailNoteResponse;
import com.skilize.interview.presentation.response.InterviewResponse;
import com.skilize.user.domain.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 面談メモの REST API コントローラー（TL/ADMIN 向け）。
 * 棚卸IDをキーに面談メモを取得・保存する。ロール制御は Service 層で行う。
 */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /**
     * 指定棚卸の面談メモを取得する。
     * 面談メモがまだ作成されていない場合は 404 を返す（空のメモは作成しない設計）。
     */
    @GetMapping("/inventory/{inventoryId}")
    public ResponseEntity<InterviewResponse> getMine(@PathVariable int inventoryId,
                                                      @AuthenticationPrincipal(expression = "user") User user) {
        // 存在する場合のみ明細ノートを取得して200を返す
        Optional<InventoryInterview> interviewOptional = interviewService.findMine(inventoryId, user);
        if (interviewOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InventoryInterview interview = interviewOptional.get();
        List<DetailNoteResponse> notes = buildDetailNoteResponses(interview.getId());
        return ResponseEntity.ok(InterviewResponse.from(interview, inventoryId, notes));
    }

    /**
     * 面談メモを保存する（upsert）。ヘッダー（全体備忘録）と明細ノートをまとめて保存する。
     * 既存レコードがあれば更新、なければ新規作成する（InterviewService.save() で制御）。
     */
    @PutMapping("/inventory/{inventoryId}")
    public InterviewResponse save(@PathVariable int inventoryId,
                                   @AuthenticationPrincipal(expression = "user") User user,
                                   @RequestBody @Valid SaveInterviewRequest req) {
        List<DetailNoteCommand> commands = new ArrayList<>();
        for (DetailNoteRequest d : req.detailNotes()) {
            commands.add(new DetailNoteCommand(d.detailType(), d.detailId(), d.note()));
        }
        InventoryInterview saved = interviewService.save(inventoryId, user, req.generalNote(), commands);
        // 保存後に明細ノートを再取得してレスポンスに付与する
        List<DetailNoteResponse> notes = buildDetailNoteResponses(saved.getId());
        return InterviewResponse.from(saved, inventoryId, notes);
    }

    /**
     * 前年度の面談メモを取得する。今年度棚卸の比較・参照用途。
     * 前年度棚卸が存在しないか、面談メモが未作成の場合は 404 を返す。
     */
    @GetMapping("/inventory/{inventoryId}/prev-year")
    public ResponseEntity<InterviewResponse> getPrevYear(@PathVariable int inventoryId,
                                                          @AuthenticationPrincipal(expression = "user") User user) {
        Optional<InventoryInterview> interviewOptional = interviewService.findPrevYear(inventoryId, user);
        if (interviewOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InventoryInterview interview = interviewOptional.get();
        List<DetailNoteResponse> notes = buildDetailNoteResponses(interview.getId());
        return ResponseEntity.ok(InterviewResponse.from(interview, inventoryId, notes));
    }

    /** 明細ノート一覧を取得し、レスポンス型に変換する。 */
    private List<DetailNoteResponse> buildDetailNoteResponses(int interviewId) {
        List<DetailNoteResponse> notes = new ArrayList<>();
        for (InterviewDetailNote note : interviewService.findDetailNotes(interviewId)) {
            notes.add(DetailNoteResponse.from(note));
        }
        return notes;
    }
}

package com.skilize.interview.application;

import com.skilize.interview.application.command.DetailNoteCommand;
import com.skilize.interview.domain.*;
import com.skilize.inventory.domain.Inventory;
import com.skilize.inventory.domain.InventoryRepository;
import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * 面談メモの保存・取得ビジネスロジック。TL/ADMIN のみ操作可。
 * 面談メモは interviewer_id（面談実施者）ごとに管理し、他者のメモへのアクセスは許可しない。
 * 明細ノートは全件洗い替え（先に全削除→再 INSERT）で更新する。
 */
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InventoryInterviewRepository inventoryInterviewRepository;
    private final InterviewDetailNoteRepository interviewDetailNoteRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * 指定棚卸に対して、リクエスト者（TL/ADMIN）が記録した面談メモを取得する。
     * 面談メモは面談者ごとに独立して管理されるため、他の TL のメモは取得できない。
     */
    @Transactional(readOnly = true)
    public Optional<InventoryInterview> findMine(int inventoryId, User requester) {
        requireTlOrAdmin(requester);
        return inventoryInterviewRepository.findByInventoryIdAndInterviewerId(inventoryId, requester.getId());
    }

    /**
     * 面談メモ（全体備忘録 + 明細ノート）を保存する。
     * 既存メモがあれば更新（upsert）、なければ新規作成する。明細ノートは全件洗い替えで更新する。
     */
    @Transactional
    public InventoryInterview save(int inventoryId, User requester,
                                    String generalNote,
                                    List<DetailNoteCommand> detailNotes) {
        requireTlOrAdmin(requester);

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "棚卸が見つかりません"));

        // upsert パターン: 既存レコードがあれば取得、なければ新規作成する
        // orElseGet(): 値が存在しない場合のみラムダを実行する（存在する場合はDBアクセスしない）
        InventoryInterview interview = inventoryInterviewRepository
                .findByInventoryIdAndInterviewerId(inventoryId, requester.getId())
                .orElseGet(() -> InventoryInterview.create(inventory, requester, generalNote));

        interview.update(generalNote);
        InventoryInterview saved = inventoryInterviewRepository.save(interview);

        // 明細ノートは全件洗い替え（全削除 → 再 INSERT）
        interviewDetailNoteRepository.deleteByInterviewId(saved.getId());
        List<InterviewDetailNote> notes = detailNotes.stream()
                .map(cmd -> InterviewDetailNote.create(saved, cmd.detailType(), cmd.detailId(), cmd.note()))
                .toList();
        interviewDetailNoteRepository.saveAll(notes);

        return saved;
    }

    /**
     * 指定棚卸の前年度棚卸に対するリクエスト者の面談メモを取得する。
     * 前年度比較・振り返りの際にTLが前年度に書いたメモを参照するために使う。
     */
    @Transactional(readOnly = true)
    public Optional<InventoryInterview> findPrevYear(int inventoryId, User requester) {
        requireTlOrAdmin(requester);

        Inventory current = inventoryRepository.findByIdWithAssociations(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "棚卸が見つかりません"));

        List<Inventory> all = inventoryRepository.findByUserIdWithFiscalYear(current.getUser().getId());

        // findByUserIdWithFiscalYear は startDate DESC 順。current の年度開始日より前のものを前年度とする
        Optional<Inventory> prevInventory = all.stream()
                .filter(inv -> inv.getFiscalYear().getStartDate()
                        .isBefore(current.getFiscalYear().getStartDate()))
                .findFirst();

        // flatMap: prevInventory が存在する場合のみ面談メモを検索し、どちらか一方でも空なら Optional.empty() を返す
        return prevInventory.flatMap(prev ->
                inventoryInterviewRepository.findByInventoryIdAndInterviewerId(prev.getId(), requester.getId()));
    }

    /** 指定面談メモ（interview_id）に紐づく明細ノートを全件取得する。 */
    public List<InterviewDetailNote> findDetailNotes(int interviewId) {
        return interviewDetailNoteRepository.findByInterviewId(interviewId);
    }

    /**
     * TL/ADMIN 以外のアクセスを弾くガード。
     * コントローラーの @PreAuthorize でも制御しているが、サービス層でも二重チェックする。
     */
    private void requireTlOrAdmin(User user) {
        if (user.getRole() != Role.TL && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "INTERVIEW_ACCESS_DENIED");
        }
    }

}

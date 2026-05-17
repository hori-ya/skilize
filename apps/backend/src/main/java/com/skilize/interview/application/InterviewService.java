package com.skilize.interview.application;

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

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InventoryInterviewRepository inventoryInterviewRepository;
    private final InterviewDetailNoteRepository interviewDetailNoteRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public Optional<InventoryInterview> findMine(int inventoryId, User requester) {
        requireTlOrAdmin(requester);
        return inventoryInterviewRepository.findByInventoryIdAndInterviewerId(inventoryId, requester.getId());
    }

    @Transactional
    public InventoryInterview save(int inventoryId, User requester,
                                    String generalNote,
                                    List<DetailNoteItem> detailNotes) {
        requireTlOrAdmin(requester);

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "棚卸が見つかりません"));

        InventoryInterview interview = inventoryInterviewRepository
                .findByInventoryIdAndInterviewerId(inventoryId, requester.getId())
                .orElseGet(() -> InventoryInterview.create(inventory, requester, generalNote));

        interview.update(generalNote);
        InventoryInterview saved = inventoryInterviewRepository.save(interview);

        interviewDetailNoteRepository.deleteByInterviewId(saved.getId());
        List<InterviewDetailNote> notes = detailNotes.stream()
                .map(item -> InterviewDetailNote.create(saved, item.detailType(), item.detailId(), item.note()))
                .toList();
        interviewDetailNoteRepository.saveAll(notes);

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<InventoryInterview> findPrevYear(int inventoryId, User requester) {
        requireTlOrAdmin(requester);

        Inventory current = inventoryRepository.findByIdWithAssociations(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "棚卸が見つかりません"));

        List<Inventory> all = inventoryRepository.findByUserIdWithFiscalYear(current.getUser().getId());

        // findByUserIdWithFiscalYear は startDate DESC 順。current の次の要素が前年度
        Optional<Inventory> prevInventory = all.stream()
                .filter(inv -> inv.getFiscalYear().getStartDate()
                        .isBefore(current.getFiscalYear().getStartDate()))
                .findFirst();

        return prevInventory.flatMap(prev ->
                inventoryInterviewRepository.findByInventoryIdAndInterviewerId(prev.getId(), requester.getId()));
    }

    public List<InterviewDetailNote> findDetailNotes(int interviewId) {
        return interviewDetailNoteRepository.findByInterviewId(interviewId);
    }

    private void requireTlOrAdmin(User user) {
        if (user.getRole() != Role.TL && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "面談メモへのアクセス権限がありません");
        }
    }

    public record DetailNoteItem(DetailType detailType, Integer detailId, String note) {}
}

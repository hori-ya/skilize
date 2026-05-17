package com.skilize.interview.presentation;

import com.skilize.interview.application.InterviewService;
import com.skilize.interview.domain.InventoryInterview;
import com.skilize.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/inventory/{inventoryId}")
    public ResponseEntity<InterviewResponse> getMine(@PathVariable int inventoryId,
                                                      @AuthenticationPrincipal User user) {
        return interviewService.findMine(inventoryId, user)
                .map(interview -> {
                    List<DetailNoteResponse> notes = interviewService.findDetailNotes(interview.getId())
                            .stream().map(DetailNoteResponse::from).toList();
                    return ResponseEntity.ok(InterviewResponse.from(interview, inventoryId, notes));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/inventory/{inventoryId}")
    public InterviewResponse save(@PathVariable int inventoryId,
                                   @AuthenticationPrincipal User user,
                                   @RequestBody @Valid SaveInterviewRequest req) {
        List<InterviewService.DetailNoteItem> items = req.detailNotes().stream()
                .map(d -> new InterviewService.DetailNoteItem(d.detailType(), d.detailId(), d.note()))
                .toList();
        InventoryInterview saved = interviewService.save(
                inventoryId, user, req.generalNote(), items);
        List<DetailNoteResponse> notes = interviewService.findDetailNotes(saved.getId())
                .stream().map(DetailNoteResponse::from).toList();
        return InterviewResponse.from(saved, inventoryId, notes);
    }

    @GetMapping("/inventory/{inventoryId}/prev-year")
    public ResponseEntity<InterviewResponse> getPrevYear(@PathVariable int inventoryId,
                                                          @AuthenticationPrincipal User user) {
        return interviewService.findPrevYear(inventoryId, user)
                .map(interview -> {
                    List<DetailNoteResponse> notes = interviewService.findDetailNotes(interview.getId())
                            .stream().map(DetailNoteResponse::from).toList();
                    return ResponseEntity.ok(InterviewResponse.from(interview, inventoryId, notes));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

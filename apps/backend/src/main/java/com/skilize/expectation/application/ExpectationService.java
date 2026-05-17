package com.skilize.expectation.application;

import com.skilize.expectation.domain.UserExpectation;
import com.skilize.expectation.domain.UserExpectationRepository;
import com.skilize.expectation.presentation.ExpectationResponse;
import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import com.skilize.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ExpectationService {

    private final UserExpectationRepository expectationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ExpectationResponse getForUser(int targetUserId, User requester) {
        requireAccess(targetUserId, requester);
        return expectationRepository.findByUserId(targetUserId)
                .map(ExpectationResponse::from)
                .orElse(ExpectationResponse.empty());
    }

    @Transactional
    public ExpectationResponse saveTlExpectation(int targetUserId, User requester, String expectation) {
        // ユーザーマスタで紐付けられた担当TLのみ編集可能
        if (requester.getRole() != Role.TL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "TLが期待することはユーザーの担当TLのみ編集可能です");
        }
        User target = findUser(targetUserId);
        if (!requester.getId().equals(target.getTlUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "担当TLのみTL期待コメントを編集できます");
        }
        UserExpectation entity = expectationRepository.findByUserId(targetUserId)
                .orElseGet(() -> UserExpectation.create(target));
        entity.updateTlExpectation(expectation);
        return ExpectationResponse.from(expectationRepository.save(entity));
    }

    @Transactional
    public ExpectationResponse saveCompanyExpectation(int targetUserId, User requester, String expectation) {
        if (requester.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "会社期待の設定は管理者のみ可能です");
        }
        User target = findUser(targetUserId);
        UserExpectation entity = expectationRepository.findByUserId(targetUserId)
                .orElseGet(() -> UserExpectation.create(target));
        entity.updateCompanyExpectation(expectation);
        return ExpectationResponse.from(expectationRepository.save(entity));
    }

    private void requireAccess(int targetUserId, User requester) {
        if (requester.getRole() == Role.ADMIN) return;
        if (requester.getRole() == Role.TL) {
            User target = findUser(targetUserId);
            if (!requester.getId().equals(target.getTlUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "チームメンバーの期待情報のみアクセス可能です");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "アクセス権限がありません");
    }

    private User findUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));
    }
}

package com.skilize.user.application;

import com.skilize.fiscalyear.domain.FiscalYear;
import com.skilize.fiscalyear.domain.FiscalYearRepository;
import com.skilize.inventory.domain.Inventory;
import com.skilize.inventory.domain.InventoryRepository;
import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import com.skilize.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FiscalYearRepository fiscalYearRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public User create(String userId, String name, String email, Role role, Integer tlUserId) {
        if (userRepository.findByUserId(userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "このユーザーIDは既に使用されています");
        }
        return userRepository.save(User.create(userId, name, email, role, tlUserId,
                passwordEncoder.encode(userId)));
    }

    @Transactional
    public User update(int id, String name, String email, Role role, Integer tlUserId, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.update(name, email, role, tlUserId, active);
        return userRepository.save(user);
    }

    @Transactional
    public String resetPassword(int id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String tempPassword = user.getUserId();
        user.resetPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return tempPassword;
    }

    @Transactional(readOnly = true)
    public Optional<FiscalYear> findCurrentFiscalYear() {
        return fiscalYearRepository.findCurrent(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Optional<Inventory> findCurrentInventory(int userId, int fiscalYearId) {
        return inventoryRepository.findByUserIdAndFiscalYearId(userId, fiscalYearId);
    }
}

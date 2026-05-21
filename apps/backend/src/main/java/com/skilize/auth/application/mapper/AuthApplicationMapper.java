package com.skilize.auth.application.mapper;

import com.skilize.auth.application.command.ChangePasswordCommand;
import com.skilize.auth.application.command.LoginCommand;
import com.skilize.auth.presentation.request.ChangePasswordRequest;
import com.skilize.auth.presentation.request.LoginRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthApplicationMapper {

    public LoginCommand toCommand(LoginRequest request) {
        return new LoginCommand(request.userId(), request.password());
    }

    public ChangePasswordCommand toCommand(ChangePasswordRequest request) {
        return new ChangePasswordCommand(request.currentPassword(), request.newPassword());
    }
}

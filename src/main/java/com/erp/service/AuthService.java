package com.erp.service;

import com.erp.dto.request.CreateStaffUserRequest;
import com.erp.dto.request.ForgotPasswordRequest;
import com.erp.dto.request.LoginRequest;
import com.erp.dto.request.RefreshTokenRequest;
import com.erp.dto.request.RegisterRequest;
import com.erp.dto.request.ResetPasswordRequest;
import com.erp.dto.response.AuthResponse;
import com.erp.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    /** ADMIN-only: create a FACULTY or ADMIN account directly, pre-verified. */
    UserResponse createStaffUser(CreateStaffUserRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void verifyEmail(String token);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}

package com.erp.service.impl;

import com.erp.dto.request.CreateStaffUserRequest;
import com.erp.dto.request.ForgotPasswordRequest;
import com.erp.dto.request.LoginRequest;
import com.erp.dto.request.RefreshTokenRequest;
import com.erp.dto.request.RegisterRequest;
import com.erp.dto.request.ResetPasswordRequest;
import com.erp.dto.response.AuthResponse;
import com.erp.dto.response.UserResponse;
import com.erp.entity.Role;
import com.erp.entity.User;
import com.erp.enums.AccountStatus;
import com.erp.enums.RoleName;
import com.erp.exception.BadRequestException;
import com.erp.exception.DuplicateResourceException;
import com.erp.exception.ResourceNotFoundException;
import com.erp.exception.UnauthorizedException;
import com.erp.mapper.UserMapper;
import com.erp.repository.RoleRepository;
import com.erp.repository.UserRepository;
import com.erp.security.JwtService;
import com.erp.security.UserPrincipal;
import com.erp.service.AuthService;
import com.erp.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            RoleName parsed = parseRole(roleName);
            if (parsed != RoleName.STUDENT) {
                // Public self-registration may only ever create STUDENT accounts.
                // ADMIN and FACULTY accounts must be created by an existing ADMIN
                // via AdminUserController, never through this open endpoint.
                throw new BadRequestException(
                        "Public registration only supports the STUDENT role. "
                                + "FACULTY and ADMIN accounts must be created by an administrator.");
            }
            Role role = roleRepository.findByName(parsed)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
            roles.add(role);
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .status(AccountStatus.ACTIVE)
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .roles(roles)
                .build();

        User saved = userRepository.save(user);
        emailService.sendVerificationEmail(saved.getEmail(), saved.getFirstName(), verificationToken);

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse createStaffUser(CreateStaffUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        RoleName parsed = parseRole(request.getRole());
        if (parsed == RoleName.STUDENT) {
            throw new BadRequestException("Use /api/auth/register for STUDENT accounts");
        }
        Role role = roleRepository.findByName(parsed)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", request.getRole()));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .status(AccountStatus.ACTIVE)
                // Staff accounts are created and vouched for by an admin directly,
                // so they skip the email-verification step self-registration needs.
                .emailVerified(true)
                .roles(Set.of(role))
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "identifier", request.getUsernameOrEmail()));

        return buildAuthResponse(principal, user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        String username = jwtService.extractUsername(token);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        UserPrincipal principal = UserPrincipal.from(user);

        if (!jwtService.isRefreshToken(token) || !jwtService.isTokenValid(token, principal)) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        return buildAuthResponse(principal, user);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (user.getPasswordResetTokenExpiry() == null
                || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(UserDetails principal, User user) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtService.getAccessTokenExpirationMs())
                .user(userMapper.toResponse(user))
                .build();
    }

    private RoleName parseRole(String roleName) {
        try {
            return RoleName.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + roleName);
        }
    }
}

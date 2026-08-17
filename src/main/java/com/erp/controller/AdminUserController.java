package com.erp.controller;

import com.erp.dto.request.CreateStaffUserRequest;
import com.erp.dto.response.UserResponse;
import com.erp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Staff Accounts", description = "Admin-only creation of FACULTY and ADMIN accounts")
public class AdminUserController {

    private final AuthService authService;

    @PostMapping
    @Operation(summary = "Create a FACULTY or ADMIN account (admin only)")
    public ResponseEntity<UserResponse> createStaffUser(@Valid @RequestBody CreateStaffUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createStaffUser(request));
    }
}

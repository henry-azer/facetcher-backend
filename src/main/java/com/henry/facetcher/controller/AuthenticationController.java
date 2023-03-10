package com.henry.facetcher.controller;

import com.henry.facetcher.dto.base.request.AuthRequest;
import com.henry.facetcher.dto.base.request.RefreshTokenRequest;
import com.henry.facetcher.dto.base.response.ApiResponse;
import com.henry.facetcher.manager.JWTAuthenticationManager;
import com.henry.facetcher.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * @author Henry Azer
 * @since 04/11/2022
 */
@Slf4j
@CrossOrigin
@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final JWTAuthenticationManager jwtAuthenticationManager;
    private final UserService userService;

    @PostMapping("/log-in")
    public ApiResponse login(@Valid @RequestBody AuthRequest authRequest) {
        log.info("AuthenticationController: login() called");
        return new ApiResponse(true, LocalDateTime.now().toString(),
                "User logged in successfully.", jwtAuthenticationManager.login(authRequest));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse currentLoggedUser() {
        log.info("AuthenticationController: currentLoggedUser() called");
        return new ApiResponse(true, LocalDateTime.now().toString(),
                "Current logged user fetched successfully.", userService.getCurrentUser());
    }

    @PostMapping("/refresh-token")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.info("AuthenticationController: refreshToken() called");
        return new ApiResponse(true, LocalDateTime.now().toString(),
                "Token Refreshed successfully.", jwtAuthenticationManager.refreshToken(refreshTokenRequest));
    }

    @GetMapping("/log-out")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse logout() {
        log.info("AuthenticationController: logout() called");
        return new ApiResponse(true, LocalDateTime.now().toString(),
                "User logged out successfully.", jwtAuthenticationManager.logout());
    }
}

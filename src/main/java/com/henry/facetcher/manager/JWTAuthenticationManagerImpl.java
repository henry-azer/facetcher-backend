package com.henry.facetcher.manager;

import com.henry.facetcher.dto.base.request.AuthRequest;
import com.henry.facetcher.dto.base.request.RefreshTokenRequest;
import com.henry.facetcher.dto.base.response.AuthResponse;
import com.henry.facetcher.security.jwt.JWTAuthenticationUtil;
import com.henry.facetcher.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.persistence.EntityExistsException;

/**
 * @author Henry Azer
 * @since 04/11/2022
 */
@Slf4j
@Component
public class JWTAuthenticationManagerImpl implements JWTAuthenticationManager {
    private final JWTAuthenticationUtil jwtAuthenticationUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    @Value("${facetcher.security.jwt.accessToken.expirationMs}")
    private String JWT_ACCESS_TOKEN_EXPIRATION_MS;
    @Value("${facetcher.security.jwt.refreshToken.expirationMs}")
    private String JWT_REFRESH_TOKEN_EXPIRATION_MS;

    public JWTAuthenticationManagerImpl(JWTAuthenticationUtil jwtAuthenticationUtil, @Lazy AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService) {
        this.jwtAuthenticationUtil = jwtAuthenticationUtil;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public AuthResponse login(AuthRequest authRequest) {
        log.info("JWTAuthenticationManager: login() called");
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword()));
            return new AuthResponse(jwtAuthenticationUtil.generateAccessToken(authRequest.getEmail()), Long.valueOf(JWT_ACCESS_TOKEN_EXPIRATION_MS),
                    refreshTokenService.createRefreshToken(authRequest.getEmail()).getToken(), Long.valueOf(JWT_REFRESH_TOKEN_EXPIRATION_MS));
        } catch (EntityExistsException | AuthenticationException exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("JWTAuthenticationManager: refreshToken() called");
        return new AuthResponse(jwtAuthenticationUtil
                .generateAccessToken(refreshTokenRequest.getEmail()), Long.valueOf(JWT_ACCESS_TOKEN_EXPIRATION_MS),
                refreshTokenService.refreshToken(refreshTokenRequest).getToken(), Long.valueOf(JWT_REFRESH_TOKEN_EXPIRATION_MS));
    }

    @Override
    public Boolean logout() {
        log.info("JWTAuthenticationManager: logout() called");
        return refreshTokenService.deleteRefreshToken(getCurrentUserEmail());
    }

    @Override
    public String getCurrentUserEmail() {
        log.info("JWTAuthenticationManager: getCurrentUserEmail() called");
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
    }
}

package com.splitledger.security;

import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final CookieCsrfTokenRepository csrfTokenRepository;

    public AuthController(CookieCsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken, HttpServletRequest request, HttpServletResponse response) {
        csrfTokenRepository.saveToken(csrfToken, request, response);
        return new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName());
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal ApplicationOidcUser user) {
        return new CurrentUserResponse(user.getApplicationUserId(), user.getEmail(), user.getDisplayName());
    }

    public record CsrfResponse(String token, String headerName) { }

    public record CurrentUserResponse(UUID id, String email, String displayName) { }
}

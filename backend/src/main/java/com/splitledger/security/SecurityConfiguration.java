package com.splitledger.security;

import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfiguration {

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> registrationRepository,
            CookieCsrfTokenRepository csrfTokenRepository,
            GoogleOidcUserService googleOidcUserService,
            @Value("${APP_FRONTEND_URL:http://localhost:5173}") String frontendUrl) throws Exception {

        Optional<ClientRegistrationRepository> clientRegistrations =
                Optional.ofNullable(registrationRepository.getIfAvailable());

        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/health", "/api/v1/auth/csrf", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));

        clientRegistrations.ifPresent(repository -> {
            http.setSharedObject(ClientRegistrationRepository.class, repository);
            try {
                http.oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
                        .successHandler((request, response, authentication) ->
                                response.sendRedirect(frontendUrl)));
            } catch (Exception exception) {
                throw new IllegalStateException("Could not configure Google OIDC login", exception);
            }
        });

        return http.build();
    }
}

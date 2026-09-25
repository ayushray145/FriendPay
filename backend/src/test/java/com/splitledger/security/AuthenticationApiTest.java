package com.splitledger.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitledger.user.AppUser;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublicButCurrentUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfEndpointProvidesTokenAndExpectedRequestHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    @Test
    void currentUserReturnsApplicationIdentityFromAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").with(authentication(applicationAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ayush@example.com"))
                .andExpect(jsonPath("$.displayName").value("Ayush"));
    }

    @Test
    void logoutRequiresCsrfAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(authentication(applicationAuthentication())))
                .andExpect(status().isForbidden());

        MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfJson = csrfResponse.getResponse().getContentAsString();
        String csrfToken = JsonPath.read(csrfJson, "$.token");
        String headerName = JsonPath.read(csrfJson, "$.headerName");
        Cookie csrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(authentication(applicationAuthentication()))
                        .cookie(csrfCookie)
                        .header(headerName, csrfToken))
                .andExpect(status().isNoContent());
    }

    private Authentication applicationAuthentication() {
        Instant now = Instant.now();
        Map<String, Object> claims = Map.of(
                "sub", "google-subject",
                "email", "ayush@example.com",
                "name", "Ayush");
        OidcIdToken idToken = new OidcIdToken("test-id-token", now, now.plusSeconds(300), claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);
        DefaultOidcUser googleUser = new DefaultOidcUser(List.of(), idToken, userInfo);
        ApplicationOidcUser principal = new ApplicationOidcUser(
                new AppUser("google", "google-subject", "ayush@example.com", "Ayush"), googleUser);
        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }
}

package com.splitledger.friend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.splitledger.security.ApplicationOidcUser;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class FriendApiTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository appUserRepository;

    @Test
    @Transactional
    void userCanRequestAndAcceptFriendshipWithPrivateNicknames() throws Exception {
        AppUser requester = createUser("requester", "Requester");
        AppUser recipient = createUser("recipient", "Recipient");
        Authentication requesterAuth = authenticationFor(requester);
        Authentication recipientAuth = authenticationFor(recipient);

        String sent = mockMvc.perform(post("/api/v1/friends/requests")
                        .with(authentication(requesterAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + recipient.getEmail() + "\",\"nickname\":\"Pal\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direction").value("OUTGOING"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.nickname").value("Pal"))
                .andReturn().getResponse().getContentAsString();
        String requestId = JsonPath.read(sent, "$.id");

        mockMvc.perform(get("/api/v1/friends/requests").with(authentication(recipientAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].direction").value("INCOMING"))
                .andExpect(jsonPath("$[0].email").value(requester.getEmail()));

        mockMvc.perform(post("/api/v1/friends/requests/{id}/accept", requestId)
                        .with(authentication(recipientAuth)).with(validCsrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Requester"));

        mockMvc.perform(get("/api/v1/friends").with(authentication(requesterAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nickname").value("Pal"));
        mockMvc.perform(get("/api/v1/friends").with(authentication(recipientAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nickname").value("Requester"));
        mockMvc.perform(get("/api/v1/people").with(authentication(requesterAuth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].displayName").value("Pal"));
        mockMvc.perform(get("/api/v1/people").with(authentication(recipientAuth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].displayName").value("Requester"));

        mockMvc.perform(patch("/api/v1/friends/{id}/nickname", requestId)
                        .with(authentication(recipientAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"Study buddy\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nickname").value("Study buddy"));
        mockMvc.perform(get("/api/v1/friends").with(authentication(requesterAuth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nickname").value("Pal"));
        mockMvc.perform(get("/api/v1/people").with(authentication(recipientAuth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].displayName").value("Study buddy"));
    }

    @Test
    @Transactional
    void preventsSelfAndDuplicateRequestsAndHidesOtherUsersRequests() throws Exception {
        AppUser requester = createUser("sender", "Sender");
        AppUser recipient = createUser("target", "Target");
        AppUser outsider = createUser("outsider", "Outsider");
        Authentication requesterAuth = authenticationFor(requester);
        String body = "{\"email\":\"" + recipient.getEmail() + "\"}";

        mockMvc.perform(post("/api/v1/friends/requests").with(authentication(requesterAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/friends/requests").with(authentication(requesterAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/friends/requests").with(authentication(requesterAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + requester.getEmail() + "\"}"))
                .andExpect(status().isConflict());

        String requestId = JsonPath.read(mockMvc.perform(get("/api/v1/friends/requests")
                        .with(authentication(requesterAuth))).andReturn().getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(post("/api/v1/friends/requests/{id}/accept", requestId)
                        .with(authentication(authenticationFor(outsider))).with(validCsrfToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void friendEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/friends")).andExpect(status().isUnauthorized());
    }

    private AppUser createUser(String label, String displayName) {
        String unique = label + "-" + UUID.randomUUID();
        return appUserRepository.saveAndFlush(new AppUser("google", unique, unique + "@example.com", displayName));
    }

    private Authentication authenticationFor(AppUser appUser) {
        Instant now = Instant.now();
        Map<String, Object> claims = Map.of("sub", appUser.getAuthProviderSubject(),
                "email", appUser.getEmail(), "name", appUser.getDisplayName());
        OidcIdToken token = new OidcIdToken("test-id-token", now, now.plusSeconds(300), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(), token, new OidcUserInfo(claims));
        ApplicationOidcUser principal = new ApplicationOidcUser(appUser, oidcUser);
        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }

    private RequestPostProcessor validCsrfToken() {
        String token = UUID.randomUUID().toString();
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", token));
            request.addHeader("X-CSRF-TOKEN", token);
            return request;
        };
    }
}

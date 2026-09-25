package com.splitledger.person;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class PeopleApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PersonRepository personRepository;

    @Test
    @Transactional
    void authenticatedUserCanCreateListAndReadTheirPerson() throws Exception {
        AppUser owner = createUser("owner");
        Authentication authentication = authenticationFor(owner);

        var created = mockMvc.perform(post("/api/v1/people")
                        .with(authentication(authentication))
                        .with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"  Rahul  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Rahul"))
                .andExpect(header().exists("Location"))
                .andReturn();

        String responseBody = created.getResponse().getContentAsString();
        String personId = JsonPath.read(responseBody, "$.id");

        mockMvc.perform(get("/api/v1/people").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(personId))
                .andExpect(jsonPath("$[0].displayName").value("Rahul"));

        mockMvc.perform(get("/api/v1/people/{personId}", personId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(personId))
                .andExpect(jsonPath("$.displayName").value("Rahul"));
    }

    @Test
    @Transactional
    void anotherAccountCannotReadSomeoneElsesPerson() throws Exception {
        AppUser owner = createUser("owner");
        AppUser otherUser = createUser("other");
        Person person = personRepository.saveAndFlush(new Person(owner, null, "Private Contact"));

        mockMvc.perform(get("/api/v1/people/{personId}", person.getId())
                        .with(authentication(authenticationFor(otherUser))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Person not found"));
    }

    @Test
    void personEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/people"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void blankDisplayNameReturnsValidationError() throws Exception {
        Authentication authentication = authenticationFor(createUser("owner"));

        mockMvc.perform(post("/api/v1/people")
                        .with(authentication(authentication))
                        .with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Add a name or a valid phone number"));
    }

    @Test
    @Transactional
    void phoneOnlyContactIsPrivateAndIndianLocalNumberIsNormalized() throws Exception {
        Authentication authentication = authenticationFor(createUser("phone-owner"));

        mockMvc.perform(post("/api/v1/people")
                        .with(authentication(authentication)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"9876543210\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("+919876543210"))
                .andExpect(jsonPath("$.phoneNumber").value("+919876543210"));

        mockMvc.perform(post("/api/v1/people")
                        .with(authentication(authentication)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    private AppUser createUser(String label) {
        String unique = label + "-" + UUID.randomUUID();
        return appUserRepository.saveAndFlush(
                new AppUser("google", unique, unique + "@example.com", label));
    }

    private Authentication authenticationFor(AppUser appUser) {
        Instant now = Instant.now();
        Map<String, Object> claims = Map.of(
                "sub", appUser.getAuthProviderSubject(),
                "email", appUser.getEmail(),
                "name", appUser.getDisplayName());
        OidcIdToken idToken = new OidcIdToken("test-id-token", now, now.plusSeconds(300), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(), idToken, new OidcUserInfo(claims));
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

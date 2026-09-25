package com.splitledger.group;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class GroupApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository userRepository;

    @Test
    @Transactional
    void ownerAddsMemberImmediatelyAndMemberCanDisputeAnExpense() throws Exception {
        AppUser owner = createUser("owner");
        AppUser member = createUser("member");
        AppUser outsider = createUser("outsider");
        Authentication ownerAuth = authenticationFor(owner);
        Authentication memberAuth = authenticationFor(member);

        String groupId = createGroup(owner);
        mockMvc.perform(post("/api/v1/groups/{groupId}/members", groupId)
                        .with(authentication(ownerAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + member.getEmail() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(get("/api/v1/groups/{groupId}", groupId).with(authentication(memberAuth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.members", org.hamcrest.Matchers.hasSize(2)));
        mockMvc.perform(get("/api/v1/group-invitations").with(authentication(memberAuth)))
                .andExpect(status().isNotFound());

        var createdExpense = mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .with(authentication(memberAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1250.50,\"description\":\"Train tickets\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paidByUserId").value(member.getId().toString()))
                .andExpect(header().exists("Location"))
                .andReturn();
        String expenseId = JsonPath.read(createdExpense.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/groups/{groupId}/disputes", groupId)
                        .with(authentication(memberAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issueType\":\"INCORRECT_AMOUNT\",\"groupExpenseId\":\"" + expenseId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issueType").value("INCORRECT_AMOUNT"))
                .andExpect(jsonPath("$.groupExpenseId").value(expenseId));
        mockMvc.perform(get("/api/v1/groups/{groupId}/disputes", groupId).with(authentication(ownerAuth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].raisedByName").value(member.getDisplayName()));
        mockMvc.perform(get("/api/v1/groups/{groupId}/disputes", groupId).with(authentication(outsiderAuth(outsider))))
                .andExpect(status().isNotFound());

        String disputeId = JsonPath.read(mockMvc.perform(get("/api/v1/groups/{groupId}/disputes", groupId)
                        .with(authentication(ownerAuth))).andReturn().getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(get("/api/v1/groups/{groupId}/disputes/{disputeId}", groupId, disputeId)
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/groups/{groupId}/disputes/{disputeId}/resolve", groupId, disputeId)
                        .with(authentication(memberAuth)).with(validCsrfToken()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/groups/{groupId}/disputes/{disputeId}/resolve", groupId, disputeId)
                        .with(authentication(ownerAuth)).with(validCsrfToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @Transactional
    void onlyOwnerCanAddOrRemoveMembersAndRemovedMembersLoseAccess() throws Exception {
        AppUser owner = createUser("owner");
        AppUser member = createUser("member");
        String groupId = createGroup(owner);
        Authentication ownerAuth = authenticationFor(owner);
        Authentication memberAuth = authenticationFor(member);
        addMember(ownerAuth, groupId, member.getEmail());

        mockMvc.perform(post("/api/v1/groups/{groupId}/members", groupId)
                        .with(authentication(memberAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + owner.getEmail() + "\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{memberId}", groupId, member.getId())
                        .with(authentication(memberAuth)).with(validCsrfToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{memberId}", groupId, member.getId())
                        .with(authentication(ownerAuth)).with(validCsrfToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/groups/{groupId}", groupId).with(authentication(memberAuth)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void supportsWronglyAddedDisputeAndValidatesIssueTargets() throws Exception {
        AppUser owner = createUser("owner");
        AppUser member = createUser("member");
        String groupId = createGroup(owner);
        addMember(authenticationFor(owner), groupId, member.getEmail());
        Authentication memberAuth = authenticationFor(member);

        mockMvc.perform(post("/api/v1/groups/{groupId}/disputes", groupId)
                        .with(authentication(memberAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"issueType\":\"WRONGLY_ADDED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issueType").value("WRONGLY_ADDED"));
        mockMvc.perform(post("/api/v1/groups/{groupId}/disputes", groupId)
                        .with(authentication(memberAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"issueType\":\"INCORRECT_AMOUNT\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/groups/{groupId}/disputes", groupId)
                        .with(authentication(memberAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"issueType\":\"NOT_A_SUPPORTED_ISSUE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void validatesGroupNamesAndExpenseAmountsAndRequiresAuthentication() throws Exception {
        AppUser owner = createUser("owner");
        Authentication auth = authenticationFor(owner);
        mockMvc.perform(post("/api/v1/groups").with(authentication(auth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        String groupId = createGroup(owner);
        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .with(authentication(auth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0,\"description\":\"Invalid\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/groups")).andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void splitsExpenseAcrossThreeParticipantsAndRoundsPenniesExactly() throws Exception {
        AppUser owner = createUser("rounding-owner");
        AppUser second = createUser("rounding-second");
        AppUser third = createUser("rounding-third");
        String groupId = createGroup(owner);
        Authentication ownerAuth = authenticationFor(owner);
        addMember(ownerAuth, groupId, second.getEmail());
        addMember(ownerAuth, groupId, third.getEmail());

        var result = mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .with(authentication(ownerAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"description\":\"Dinner\",\"participantUserIds\":[\""
                                + owner.getId() + "\",\"" + second.getId() + "\",\"" + third.getId() + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shares", org.hamcrest.Matchers.hasSize(3)))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(JsonPath.<List<Double>>read(
                result.getResponse().getContentAsString(), "$.shares[*].shareAmount"))
                .containsExactlyInAnyOrder(3.34, 3.33, 3.33);

        mockMvc.perform(get("/api/v1/groups/{groupId}/balances", groupId).with(authentication(authenticationFor(second))))
                .andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)));
    }

    @Test
    @Transactional
    void defaultsToFiveMembersAndAllowsPayerToBeExcludedFromParticipants() throws Exception {
        AppUser owner = createUser("split-owner");
        List<AppUser> members = List.of(createUser("split-a"), createUser("split-b"),
                createUser("split-c"), createUser("split-d"));
        String groupId = createGroup(owner);
        Authentication ownerAuth = authenticationFor(owner);
        for (AppUser member : members) addMember(ownerAuth, groupId, member.getEmail());

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .with(authentication(ownerAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000.01,\"description\":\"Five person meal\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shares", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$.shares[*].shareAmount", org.hamcrest.Matchers.hasItem(200.01)));

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .with(authentication(ownerAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"description\":\"Owner not sharing\",\"participantUserIds\":[\""
                                + members.get(0).getId() + "\",\"" + members.get(1).getId() + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shares", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.shares[*].shareAmount", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(5.00))));
    }

    @Test
    @Transactional
    void rejectsDuplicateAndNonMemberParticipantsAndHidesBalancesFromOutsiders() throws Exception {
        AppUser owner = createUser("validation-owner");
        AppUser member = createUser("validation-member");
        AppUser outsider = createUser("validation-outsider");
        String groupId = createGroup(owner);
        addMember(authenticationFor(owner), groupId, member.getEmail());
        Authentication ownerAuth = authenticationFor(owner);

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .with(authentication(ownerAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10,\"description\":\"Bad participants\",\"participantUserIds\":[\""
                                + member.getId() + "\",\"" + member.getId() + "\"]}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .with(authentication(ownerAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10,\"description\":\"Bad participant\",\"participantUserIds\":[\""
                                + outsider.getId() + "\"]}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/groups/{groupId}/balances", groupId)
                        .with(authentication(authenticationFor(outsider))))
                .andExpect(status().isNotFound());
    }

    private String createGroup(AppUser owner) throws Exception {
        var result = mockMvc.perform(post("/api/v1/groups").with(authentication(authenticationFor(owner)))
                        .with(validCsrfToken()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Group\"}"))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void addMember(Authentication ownerAuth, String groupId, String email) throws Exception {
        mockMvc.perform(post("/api/v1/groups/{groupId}/members", groupId)
                        .with(authentication(ownerAuth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk());
    }

    private AppUser createUser(String label) {
        String unique = label + "-" + UUID.randomUUID();
        return userRepository.saveAndFlush(new AppUser("google", unique, unique + "@example.com", label));
    }

    private Authentication authenticationFor(AppUser appUser) {
        Instant now = Instant.now();
        Map<String, Object> claims = Map.of("sub", appUser.getAuthProviderSubject(), "email", appUser.getEmail(),
                "name", appUser.getDisplayName());
        OidcIdToken idToken = new OidcIdToken("test-id-token", now, now.plusSeconds(300), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(), idToken, new OidcUserInfo(claims));
        ApplicationOidcUser principal = new ApplicationOidcUser(appUser, oidcUser);
        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }

    private Authentication outsiderAuth(AppUser outsider) { return authenticationFor(outsider); }

    private RequestPostProcessor validCsrfToken() {
        String token = UUID.randomUUID().toString();
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", token));
            request.addHeader("X-CSRF-TOKEN", token);
            return request;
        };
    }
}

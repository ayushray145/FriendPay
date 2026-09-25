package com.splitledger.settlement;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitledger.expense.Expense;
import com.splitledger.expense.ExpenseRepository;
import com.splitledger.ledger.DebtDirection;
import com.splitledger.person.Person;
import com.splitledger.person.PersonRepository;
import com.splitledger.security.ApplicationOidcUser;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
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
class SettlementApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private ExpenseRepository expenseRepository;

    @Test
    @Transactional
    void recordsPartialAndFullSettlementsAndUpdatesNetBalanceAndLedger() throws Exception {
        AppUser owner = createUser("owner");
        Person person = createPerson(owner, "Rahul");
        expenseRepository.saveAndFlush(new Expense(owner, person, new BigDecimal("100.00"),
                DebtDirection.PERSON_OWES_USER, "Dinner", Instant.parse("2025-01-01T10:00:00Z")));
        expenseRepository.saveAndFlush(new Expense(owner, person, new BigDecimal("40.00"),
                DebtDirection.USER_OWES_PERSON, "Cab", Instant.parse("2025-01-02T10:00:00Z")));
        Authentication authentication = authenticationFor(owner);

        mockMvc.perform(createSettlement(person, authentication,
                        "{\"amount\":30.00,\"paymentDirection\":\"PERSON_OWES_USER\",\"settledAt\":\"2025-02-01T10:00:00Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(30.00))
                .andExpect(jsonPath("$.paymentDirection").value("PERSON_OWES_USER"))
                .andExpect(header().exists("Location"));
        mockMvc.perform(get("/api/v1/people/{personId}/balance", person.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.netBalance").value(30.00));

        mockMvc.perform(createSettlement(person, authentication,
                        "{\"amount\":70.00,\"paymentDirection\":\"PERSON_OWES_USER\",\"settledAt\":\"2025-03-01T10:00:00Z\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/people/{personId}/balance", person.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.netBalance").value(-40.00));

        mockMvc.perform(createSettlement(person, authentication,
                        "{\"amount\":40.00,\"paymentDirection\":\"USER_OWES_PERSON\",\"settledAt\":\"2025-04-01T10:00:00Z\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/people/{personId}/balance", person.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.netBalance").value(0.00));

        mockMvc.perform(get("/api/v1/people/{personId}/settlements", person.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)));
        mockMvc.perform(get("/api/v1/people/{personId}/ledger", person.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$[0].type").value("SETTLEMENT"))
                .andExpect(jsonPath("$[0].direction").value("USER_OWES_PERSON"))
                .andExpect(jsonPath("$[3].type").value("EXPENSE"));
    }

    @Test
    @Transactional
    void rejectsOverSettlementWithoutChangingBalance() throws Exception {
        AppUser owner = createUser("owner");
        Person person = createPerson(owner, "Asha");
        expenseRepository.saveAndFlush(new Expense(owner, person, new BigDecimal("25.00"),
                DebtDirection.PERSON_OWES_USER, "Lunch", Instant.now()));
        Authentication authentication = authenticationFor(owner);

        mockMvc.perform(createSettlement(person, authentication,
                        "{\"amount\":25.01,\"paymentDirection\":\"PERSON_OWES_USER\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(createSettlement(person, authentication,
                        "{\"amount\":0,\"paymentDirection\":\"PERSON_OWES_USER\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/people/{personId}/balance", person.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.netBalance").value(25.00));
    }

    @Test
    @Transactional
    void settlementHistoryAndCreationAreScopedToTheContactOwner() throws Exception {
        AppUser owner = createUser("owner");
        AppUser other = createUser("other");
        Person person = createPerson(owner, "Private");
        expenseRepository.saveAndFlush(new Expense(owner, person, new BigDecimal("10.00"),
                DebtDirection.PERSON_OWES_USER, "Meal", Instant.now()));

        mockMvc.perform(get("/api/v1/people/{personId}/settlements", person.getId())
                        .with(authentication(authenticationFor(other))))
                .andExpect(status().isNotFound());
        mockMvc.perform(createSettlement(person, authenticationFor(other),
                        "{\"amount\":5.00,\"paymentDirection\":\"PERSON_OWES_USER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void settlementEndpointsRequireAuthentication() throws Exception {
        UUID personId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/people/{personId}/settlements", personId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/people/{personId}/ledger", personId))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createSettlement(
            Person person, Authentication authentication, String body) {
        return post("/api/v1/people/{personId}/settlements", person.getId())
                .with(authentication(authentication)).with(validCsrfToken())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private RequestPostProcessor validCsrfToken() {
        String token = UUID.randomUUID().toString();
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", token));
            request.addHeader("X-CSRF-TOKEN", token);
            return request;
        };
    }

    private AppUser createUser(String label) {
        String unique = label + "-" + UUID.randomUUID();
        return appUserRepository.saveAndFlush(new AppUser("google", unique, unique + "@example.com", label));
    }

    private Person createPerson(AppUser owner, String name) {
        return personRepository.saveAndFlush(new Person(owner, null, name));
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
}

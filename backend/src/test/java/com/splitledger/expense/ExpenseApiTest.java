package com.splitledger.expense;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.splitledger.ledger.DebtDirection;
import com.splitledger.person.Person;
import com.splitledger.person.PersonRepository;
import com.splitledger.security.ApplicationOidcUser;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    @Transactional
    void createsBothDebtDirectionsAndReturnsHistoryAndNetBalance() throws Exception {
        AppUser owner = createUser("owner");
        Person person = createPerson(owner, "Rahul");
        Authentication authentication = authenticationFor(owner);

        mockMvc.perform(createExpense(person.getId(), authentication, "{\"amount\":600.00,\"description\":\"Dinner\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(600.00))
                .andExpect(jsonPath("$.debtDirection").value("PERSON_OWES_USER"))
                .andExpect(jsonPath("$.description").value("Dinner"))
                .andExpect(header().exists("Location"));

        var reverseExpense = mockMvc.perform(createExpense(person.getId(), authentication,
                        "{\"amount\":300.00,\"description\":\"Cab\",\"debtDirection\":\"USER_OWES_PERSON\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.debtDirection").value("USER_OWES_PERSON"))
                .andReturn();
        String reverseExpenseId = JsonPath.read(reverseExpense.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/people/{personId}/balance", person.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personId").value(person.getId().toString()))
                .andExpect(jsonPath("$.netBalance").value(300.00));

        mockMvc.perform(get("/api/v1/people/{personId}/expenses", person.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].description").value("Cab"))
                .andExpect(jsonPath("$[1].description").value("Dinner"));

        mockMvc.perform(get("/api/v1/people/{personId}/expenses/{expenseId}", person.getId(), reverseExpenseId)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Cab"));
    }

    @Test
    @Transactional
    void balanceUsesExactDecimalArithmeticAndReturnsZeroForNoExpenses() throws Exception {
        AppUser owner = createUser("owner");
        Person person = createPerson(owner, "Rohit");
        Authentication authentication = authenticationFor(owner);

        mockMvc.perform(get("/api/v1/people/{personId}/balance", person.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netBalance").value(0.00));

        mockMvc.perform(createExpense(person.getId(), authentication, "{\"amount\":0.10,\"description\":\"Tea\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(createExpense(person.getId(), authentication, "{\"amount\":0.20,\"description\":\"Snack\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(createExpense(person.getId(), authentication,
                "{\"amount\":0.05,\"description\":\"Return\",\"debtDirection\":\"USER_OWES_PERSON\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/people/{personId}/balance", person.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netBalance").value(0.25));
    }

    @Test
    @Transactional
    void expenseAndBalanceAreScopedToTheContactOwner() throws Exception {
        AppUser owner = createUser("owner");
        AppUser otherUser = createUser("other");
        Person person = createPerson(owner, "Private Contact");
        Expense expense = expenseRepository.saveAndFlush(new Expense(
                owner, person, new java.math.BigDecimal("15.00"), DebtDirection.PERSON_OWES_USER, "Meal", Instant.now()));
        Authentication otherAuthentication = authenticationFor(otherUser);

        mockMvc.perform(createExpense(person.getId(), otherAuthentication,
                        "{\"amount\":25.00,\"description\":\"Unauthorized\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/people/{personId}/expenses", person.getId())
                        .with(authentication(otherAuthentication)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/people/{personId}/balance", person.getId())
                        .with(authentication(otherAuthentication)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/people/{personId}/expenses/{expenseId}", person.getId(), expense.getId())
                        .with(authentication(otherAuthentication)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void invalidAmountOrDescriptionIsRejected() throws Exception {
        AppUser owner = createUser("owner");
        Person person = createPerson(owner, "Aman");
        Authentication authentication = authenticationFor(owner);
        List<String> invalidBodies = List.of(
                "{\"amount\":0,\"description\":\"Invalid\"}",
                "{\"amount\":-1.00,\"description\":\"Invalid\"}",
                "{\"amount\":1.234,\"description\":\"Invalid\"}",
                "{\"amount\":100000000000000000.00,\"description\":\"Invalid\"}",
                "{\"amount\":10.00,\"description\":\"   \"}",
                "{\"amount\":10.00,\"description\":\"" + "x".repeat(501) + "\"}");

        for (String body : invalidBodies) {
            mockMvc.perform(createExpense(person.getId(), authentication, body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void expenseEndpointsRequireAuthentication() throws Exception {
        UUID personId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/people/{personId}/expenses", personId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/people/{personId}/balance", personId))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createExpense(
            UUID personId, Authentication authentication, String body) {
        return post("/api/v1/people/{personId}/expenses", personId)
                .with(authentication(authentication))
                .with(validCsrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
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
        return appUserRepository.saveAndFlush(
                new AppUser("google", unique, unique + "@example.com", label));
    }

    private Person createPerson(AppUser owner, String displayName) {
        return personRepository.saveAndFlush(new Person(owner, null, displayName));
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
}

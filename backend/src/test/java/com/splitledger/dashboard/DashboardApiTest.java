package com.splitledger.dashboard;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitledger.expense.Expense;
import com.splitledger.expense.ExpenseRepository;
import com.splitledger.ledger.DebtDirection;
import com.splitledger.person.Person;
import com.splitledger.person.PersonRepository;
import com.splitledger.security.ApplicationOidcUser;
import com.splitledger.settlement.Settlement;
import com.splitledger.settlement.SettlementRepository;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private SettlementRepository settlementRepository;

    @Test
    @Transactional
    void returnsExactOwnerScopedTotalsAndOmitsZeroBalances() throws Exception {
        AppUser owner = createUser("owner");
        AppUser otherOwner = createUser("other");
        Person owesOwner = createPerson(owner, "Asha");
        Person ownerOwes = createPerson(owner, "Bala");
        Person settled = createPerson(owner, "Chitra");
        createPerson(owner, "No activity");
        Person otherAccountContact = createPerson(otherOwner, "Private contact");
        expense(owner, owesOwner, "100.75", DebtDirection.PERSON_OWES_USER);
        expense(owner, owesOwner, "0.25", DebtDirection.USER_OWES_PERSON);
        expense(owner, ownerOwes, "40.25", DebtDirection.USER_OWES_PERSON);
        expense(owner, settled, "12.00", DebtDirection.PERSON_OWES_USER);
        expense(owner, settled, "12.00", DebtDirection.USER_OWES_PERSON);
        settlementRepository.saveAndFlush(new Settlement(owner, owesOwner, new BigDecimal("20.00"),
                DebtDirection.PERSON_OWES_USER, Instant.now()));
        settlementRepository.saveAndFlush(new Settlement(owner, ownerOwes, new BigDecimal("5.00"),
                DebtDirection.USER_OWES_PERSON, Instant.now()));
        expense(otherOwner, otherAccountContact, "900.00", DebtDirection.PERSON_OWES_USER);

        mockMvc.perform(get("/api/v1/dashboard").with(authentication(authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOwedToYou").value(80.50))
                .andExpect(jsonPath("$.totalYouOwe").value(35.25))
                .andExpect(jsonPath("$.netBalance").value(45.25))
                .andExpect(jsonPath("$.people", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.people[0].displayName").value("Asha"))
                .andExpect(jsonPath("$.people[0].netBalance").value(80.50))
                .andExpect(jsonPath("$.people[1].displayName").value("Bala"))
                .andExpect(jsonPath("$.people[1].netBalance").value(-35.25));
    }

    @Test
    @Transactional
    void returnsZeroTotalsAndNoPeopleWhenLedgerIsEmpty() throws Exception {
        AppUser owner = createUser("empty");
        mockMvc.perform(get("/api/v1/dashboard").with(authentication(authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOwedToYou").value(0.00))
                .andExpect(jsonPath("$.totalYouOwe").value(0.00))
                .andExpect(jsonPath("$.netBalance").value(0.00))
                .andExpect(jsonPath("$.people", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")).andExpect(status().isUnauthorized());
    }

    private void expense(AppUser owner, Person person, String amount, DebtDirection direction) {
        expenseRepository.saveAndFlush(new Expense(owner, person, new BigDecimal(amount), direction,
                "Test entry", Instant.now()));
    }

    private AppUser createUser(String label) {
        String unique = label + "-" + UUID.randomUUID();
        return appUserRepository.saveAndFlush(new AppUser("google", unique, unique + "@example.com", label));
    }

    private Person createPerson(AppUser owner, String displayName) {
        return personRepository.saveAndFlush(new Person(owner, null, displayName));
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

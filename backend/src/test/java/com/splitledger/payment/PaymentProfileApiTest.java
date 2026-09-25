package com.splitledger.payment;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.splitledger.security.ApplicationOidcUser;
import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
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
class PaymentProfileApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository userRepository;

    @Test
    @Transactional
    void savesProfileCreatesUriAndProducesDecodableQrWithoutConfirmingPayment() throws Exception {
        AppUser user = createUser();
        Authentication auth = authenticationFor(user);

        mockMvc.perform(get("/api/v1/payment-profile").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.upiId").doesNotExist());
        mockMvc.perform(get("/api/v1/payment-profile/upi-link")
                        .param("amount", "25.50").with(authentication(auth)))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/payment-profile").with(authentication(auth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"upiId\":\"rahul.shah@okaxis\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.upiId").value("rahul.shah@okaxis"));

        mockMvc.perform(get("/api/v1/payment-profile/upi-link")
                        .param("amount", "25.50").param("note", "Dinner & cab").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.upiUri").value(
                        "upi://pay?pa=rahul.shah%40okaxis&pn=Test%20User&am=25.50&cu=INR&tn=Dinner%20%26%20cab"))
                .andReturn().getResponse().getContentAsString();

        byte[] png = mockMvc.perform(get("/api/v1/payment-profile/qr")
                        .param("amount", "25.50").param("note", "Dinner & cab").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();
        var image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        String decoded = new MultiFormatReader().decode(bitmap).getText();
        org.assertj.core.api.Assertions.assertThat(decoded).isEqualTo("upi://pay?pa=rahul.shah%40okaxis"
                + "&pn=Test%20User&am=25.50&cu=INR&tn=Dinner%20%26%20cab");

        mockMvc.perform(get("/api/v1/payment-profile/upi-link")
                        .param("amount", "0.001").with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void validatesAndDeletesOwnPaymentProfileAndProtectsItsEndpoints() throws Exception {
        AppUser user = createUser();
        Authentication auth = authenticationFor(user);
        mockMvc.perform(put("/api/v1/payment-profile").with(authentication(auth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"upiId\":\"not a upi id\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/payment-profile").with(authentication(auth)).with(validCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"upiId\":\"valid@upi\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/payment-profile").with(authentication(auth)).with(validCsrfToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/payment-profile").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.upiId").doesNotExist());
        mockMvc.perform(get("/api/v1/payment-profile")).andExpect(status().isUnauthorized());
    }

    private AppUser createUser() {
        String unique = "payment-" + UUID.randomUUID();
        return userRepository.saveAndFlush(new AppUser("google", unique, unique + "@example.com", "Test User"));
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

    private RequestPostProcessor validCsrfToken() {
        String token = UUID.randomUUID().toString();
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", token));
            request.addHeader("X-CSRF-TOKEN", token);
            return request;
        };
    }
}

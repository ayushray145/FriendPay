package com.splitledger.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

@Configuration
@Conditional(GoogleOidcEnabledCondition.class)
public class GoogleOidcClientConfiguration {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(environment.getRequiredProperty("GOOGLE_CLIENT_ID"))
                .clientSecret(environment.getRequiredProperty("GOOGLE_CLIENT_SECRET"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .issuerUri(GOOGLE_ISSUER)
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}

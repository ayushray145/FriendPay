package com.splitledger.security;

import com.splitledger.user.AppUser;
import com.splitledger.user.ApplicationUserProvisioningService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final ApplicationUserProvisioningService userProvisioningService;

    public GoogleOidcUserService(ApplicationUserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String displayName = oidcUser.getFullName();

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(email)
                || !Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("required_claim_missing"),
                    "Google must return a subject and verified email address");
        }
        if (!StringUtils.hasText(displayName)) {
            displayName = email;
        }
        if (displayName.length() > 160) {
            displayName = displayName.substring(0, 160);
        }

        AppUser appUser = userProvisioningService.findOrCreateGoogleUser(subject, email, displayName);
        return new ApplicationOidcUser(appUser, oidcUser);
    }
}

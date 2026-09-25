package com.splitledger.security;

import com.splitledger.user.AppUser;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

public class ApplicationOidcUser implements OidcUser {

    private final AppUser appUser;
    private final OidcUser oidcUser;

    public ApplicationOidcUser(AppUser appUser, OidcUser oidcUser) {
        this.appUser = appUser;
        this.oidcUser = oidcUser;
    }

    public UUID getApplicationUserId() { return appUser.getId(); }
    public String getEmail() { return appUser.getEmail(); }
    public String getDisplayName() { return appUser.getDisplayName(); }

    @Override
    public Map<String, Object> getClaims() { return oidcUser.getClaims(); }

    @Override
    public OidcUserInfo getUserInfo() { return oidcUser.getUserInfo(); }

    @Override
    public OidcIdToken getIdToken() { return oidcUser.getIdToken(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return oidcUser.getAuthorities(); }

    @Override
    public Map<String, Object> getAttributes() { return oidcUser.getAttributes(); }

    @Override
    public String getName() { return oidcUser.getName(); }
}

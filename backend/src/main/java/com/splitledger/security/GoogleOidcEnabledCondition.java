package com.splitledger.security;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class GoogleOidcEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String clientId = context.getEnvironment().getProperty("GOOGLE_CLIENT_ID");
        String clientSecret = context.getEnvironment().getProperty("GOOGLE_CLIENT_SECRET");
        return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
    }
}

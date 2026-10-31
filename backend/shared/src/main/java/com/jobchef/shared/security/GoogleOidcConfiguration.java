package com.jobchef.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@Profile("live")
public class GoogleOidcConfiguration {
    @Bean
    JwtDecoder googleOidcJwtDecoder(
            @Value("${jobchef.internal-audience:}") String audience) {
        if (audience.isBlank()) {
            throw new IllegalStateException("JOB_CHEF_INTERNAL_AUDIENCE must be configured for authenticated internal endpoints.");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .build();
        OAuth2TokenValidator<Jwt> issuer =
                JwtValidators.createDefaultWithIssuer("https://accounts.google.com");
        OAuth2TokenValidator<Jwt> audienceValidator = token ->
                token.getAudience().contains(audience)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new org.springframework.security.oauth2.core.OAuth2Error(
                                        "invalid_token", "Unexpected token audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audienceValidator));
        return decoder;
    }
}

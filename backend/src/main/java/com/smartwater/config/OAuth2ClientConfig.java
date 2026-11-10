package com.smartwater.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Google login is switched on only when GOOGLE_CLIENT_ID is set. With no client id this bean does not exist,
 * so the app starts normally (and the "Continue with Google" button is hidden).
 */
@Configuration
public class OAuth2ClientConfig {

    @Bean
    @ConditionalOnExpression("'${app.oauth2.google.client-id:}' != ''")
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.oauth2.google.client-id}") String clientId,
            @Value("${app.oauth2.google.client-secret:}") String clientSecret,
            @Value("${app.public-url:}") String publicUrl) {

        // PUBLIC_URL (for example https://water.example.com) is where the BROWSER reaches the app.
        // Register "<PUBLIC_URL>/api/login/oauth2/code/google" as the redirect URI in Google Cloud Console.
        String redirect = publicUrl.isBlank()
                ? "{baseUrl}/api/login/oauth2/code/{registrationId}"
                : publicUrl.replaceAll("/+$", "") + "/api/login/oauth2/code/{registrationId}";

        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirect)
                .scope("openid", "profile", "email")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}

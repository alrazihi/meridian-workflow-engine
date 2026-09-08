package com.meridian.infrastructure.security.config;

import com.meridian.infrastructure.web.filter.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String JWKS_URI = System.getenv().getOrDefault("JWT_JWKS_URI", "");
    private static final String SECRET_KEY_BASE64 = System.getenv().getOrDefault("JWT_SECRET_KEY", "c2VjcmV0X2tleV9mb3JfZGV2X2xpbmtlZF9pbl9sZWZ0X2RlbW9fbmV0d29yaw==");

    public SecurityConfig() {
        if (JWKS_URI.isBlank() && SECRET_KEY_BASE64.equals("c2VjcmV0X2tleV9mb3JfZGV2X2xpbmtlZF9pbl9sZWZ0X2RlbW9fbmV0d29yaw==")) {
            throw new IllegalStateException("Either JWT_JWKS_URI or JWT_SECRET_KEY must be set. Default secret is not safe for production.");
        }
    }

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorrelationIdFilter correlationIdFilter) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/webhooks/documents").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(correlationIdFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (!JWKS_URI.isBlank()) {
            return NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
        }
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY_BASE64);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}
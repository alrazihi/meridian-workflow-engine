package com.meridian.infrastructure.security.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class JwtDecoderRs256IntegrationTest {

    @Value("${JWT_JWKS_URI:}")
    private String jwksUri;

    @Autowired
    private JwtDecoder jwtDecoder;

    private RSAKey rsaKey;
    private String publicKeyJwk;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        this.rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .build();
        this.publicKeyJwk = rsaKey.toPublicJWK().toJSONString();
    }

    @Test
    void shouldDecodeRs256TokenWhenJwksUriIsSet() throws Exception {
        if (jwksUri.isBlank()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Skipping RS256 test: JWT_JWKS_URI not set");
            return;
        }

        String token = createRs256Token(rsaKey, "user-1", List.of("ROLE_OPERATOR"));

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("user-1");
        assertThat(jwt.getClaimAsStringList("scope")).contains("ROLE_OPERATOR");
    }

    @Test
    void shouldDecodeHs256TokenWhenJwksUriIsNotSet() throws Exception {
        if (!jwksUri.isBlank()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Skipping HS256 test: JWT_JWKS_URI is set");
            return;
        }

        String token = createHs256Token("user-1", List.of("ROLE_OPERATOR"));

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("user-1");
        assertThat(jwt.getClaimAsStringList("scope")).contains("ROLE_OPERATOR");
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() throws Exception {
        if (!jwksUri.isBlank()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Skipping HS256 test: JWT_JWKS_URI is set");
            return;
        }

        String token = createHs256Token("user-1", List.of("ROLE_OPERATOR"));
        String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

        assertThatThrownBy(() -> jwtDecoder.decode(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        if (!jwksUri.isBlank()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Skipping HS256 test: JWT_JWKS_URI is set");
            return;
        }

        String token = createHs256Token("user-1", List.of("ROLE_OPERATOR"), Instant.now().minusSeconds(3600));

        assertThatThrownBy(() -> jwtDecoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    private String createRs256Token(RSAKey rsaKey, String subject, List<String> scopes) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("scope", String.join(" ", scopes))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .issueTime(new Date())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                claims
        );

        RSASSASigner signer = new RSASSASigner(rsaKey);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private String createHs256Token(String subject, List<String> scopes) throws Exception {
        return createHs256Token(subject, scopes, Instant.now().plusSeconds(3600));
    }

    private String createHs256Token(String subject, List<String> scopes, Instant expiration) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("scope", String.join(" ", scopes))
                .expirationTime(Date.from(expiration))
                .issueTime(new Date())
                .build();

        com.nimbusds.jose.JWSHeader header = new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256);
        SignedJWT signedJWT = new SignedJWT(header, claims);

        String secret = System.getenv().getOrDefault("JWT_SECRET_KEY", "c2VjcmV0X2tleV9mb3JfZGV2X2xpbmtlZF9pbl9sZWZ0X2RlbW9fbmV0d29yaw==");
        byte[] keyBytes = java.util.Base64.getDecoder().decode(secret);
        com.nimbusds.jose.crypto.MACSigner signer = new com.nimbusds.jose.crypto.MACSigner(keyBytes);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}

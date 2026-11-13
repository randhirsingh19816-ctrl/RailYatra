package com.trainreservation.backend.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String ISSUER = "https://trainreservation.test";
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes());

    private JwtEncoder encoder;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(TEST_SECRET), "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        jwtService = new JwtService(encoder, decoder, Clock.systemUTC(), ISSUER, 60_000L);
    }

    @Test
    void generatedTokenValidatesIssuerAndSubject() {
        Jwt decoded = jwtService.validateToken(jwtService.generateToken("alice@example.com"));

        assertEquals(ISSUER, decoded.getIssuer().toString());
        assertEquals("alice@example.com", decoded.getSubject());
        assertEquals(60L, jwtService.getExpirationSeconds());
    }

    @Test
    void validationRejectsTamperedSignature() {
        String token = jwtService.generateToken("alice@example.com");
        int signatureStart = token.lastIndexOf('.') + 1;
        char firstSignatureCharacter = token.charAt(signatureStart);
        String tamperedToken = token.substring(0, signatureStart)
                + (firstSignatureCharacter == 'A' ? 'B' : 'A')
                + token.substring(signatureStart + 1);

        assertThrows(JwtException.class, () -> jwtService.validateToken(tamperedToken));
    }

    @Test
    void validationRejectsExpiredToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("alice@example.com")
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .build();
        String expired = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThrows(JwtException.class, () -> jwtService.validateToken(expired));
    }
}

package com.krsoliveira.pcp.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Gera e valida tokens JWT com assinatura HMAC-SHA256 (HS256).
 * O segredo e a expiração vêm de variáveis de ambiente ({@code app.jwt.*}).
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiracao-ms}") long expiracaoMs) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    /** Gera um token JWT assinado com o e-mail como subject. */
    public String gerarToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiracaoMs))
                .signWith(chave)
                .compact();
    }

    /** Extrai o e-mail (subject) do token. Lança exceção se inválido. */
    public String extrairEmail(String token) {
        return claims(token).getSubject();
    }

    /** Verifica assinatura, e-mail e expiração. */
    public boolean isValido(String token, UserDetails userDetails) {
        String email = extrairEmail(token);
        return email.equals(userDetails.getUsername()) && !isExpirado(token);
    }

    private boolean isExpirado(String token) {
        return claims(token).getExpiration().before(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

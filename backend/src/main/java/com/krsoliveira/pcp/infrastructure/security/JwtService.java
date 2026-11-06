package com.krsoliveira.pcp.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.temporal.ChronoUnit;
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

    /** Verifica assinatura, e-mail, expiração e se o token não é anterior à última mudança de senha. */
    public boolean isValido(String token, UserDetails userDetails) {
        String email = extrairEmail(token);
        return email.equals(userDetails.getUsername()) && !isExpirado(token) && !anteriorAMudancaDeSenha(token, userDetails);
    }

    /**
     * A emissão do token é guardada em segundos, por isso a comparação também é: quem entra logo depois de trocar a senha,
     * no mesmo segundo, não pode ficar de fora. O que sobra é um token emitido nesse mesmo segundo, antes da troca.
     */
    private boolean anteriorAMudancaDeSenha(String token, UserDetails userDetails) {
        if (!(userDetails instanceof UsuarioAutenticado usuario) || usuario.getSenhaAlteradaEm() == null) {
            return false;
        }
        return claims(token).getIssuedAt().toInstant().isBefore(usuario.getSenhaAlteradaEm().truncatedTo(ChronoUnit.SECONDS));
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

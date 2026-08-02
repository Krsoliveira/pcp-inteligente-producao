package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.auth.RegistrarUsuario;
import com.krsoliveira.pcp.infrastructure.security.JwtService;
import com.krsoliveira.pcp.infrastructure.security.UsuarioDetailsService;
import com.krsoliveira.pcp.infrastructure.web.dto.LoginRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.RegistrarRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints PÚBLICOS de autenticação — não exigem token JWT.
 *
 * POST /api/v1/auth/registrar — cria um novo usuário (201).
 * POST /api/v1/auth/login     — valida credenciais e devolve um JWT Bearer (200).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Registro de usuários e geração de tokens JWT")
public class AuthController {

    private final RegistrarUsuario registrarUsuario;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public AuthController(RegistrarUsuario registrarUsuario,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UsuarioDetailsService usuarioDetailsService) {
        this.registrarUsuario = registrarUsuario;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um novo usuário",
            description = "Cria a conta e devolve 201 sem corpo. Use /login para obter o token.")
    public void registrar(@Valid @RequestBody RegistrarRequest request) {
        registrarUsuario.executar(
                request.nome(), request.email(), request.senha(), request.perfil());
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica o usuário e devolve um token JWT Bearer",
            description = "Envie o token no cabeçalho: Authorization: Bearer {token}")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));
        UserDetails userDetails = usuarioDetailsService.loadUserByUsername(request.email());
        return new TokenResponse(jwtService.gerarToken(userDetails));
    }
}

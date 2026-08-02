package com.krsoliveira.pcp.domain.usuario;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de domínio: representa um usuário da plataforma PCP.
 * Classe PURA: sem anotações de framework (Spring, JPA).
 */
public class Usuario {

    private final UUID id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final Perfil perfil;
    private final Instant criadoEm;

    private Usuario(UUID id, String nome, String email, String senhaHash,
                    Perfil perfil, Instant criadoEm) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.criadoEm = criadoEm;
    }

    /**
     * Cria um novo usuário. A senha já deve chegar codificada (BCrypt);
     * a codificação é responsabilidade do caso de uso via {@link CodificadorDeSenha}.
     */
    public static Usuario criar(String nome, String email, String senhaHash, Perfil perfil) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail é obrigatório.");
        }
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }
        if (perfil == null) {
            throw new IllegalArgumentException("Perfil é obrigatório.");
        }
        return new Usuario(UUID.randomUUID(), nome.trim(), email.trim().toLowerCase(),
                senhaHash, perfil, Instant.now());
    }

    /**
     * Reconstrói um usuário existente a partir do banco de dados.
     * Não revalida invariantes: os dados já passaram por {@link #criar}.
     */
    public static Usuario reconstituir(UUID id, String nome, String email, String senhaHash,
                                       Perfil perfil, Instant criadoEm) {
        return new Usuario(id, nome, email, senhaHash, perfil, criadoEm);
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public Instant getCriadoEm() { return criadoEm; }
}

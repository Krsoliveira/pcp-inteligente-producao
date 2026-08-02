package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.usuario.Perfil;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapeamento JPA da entidade de domínio {@link Usuario}.
 * Visibilidade de pacote: ninguém fora da camada de persistência deve depender desta classe.
 */
@Entity
@Table(name = "usuario")
class UsuarioJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected UsuarioJpaEntity() {}

    static UsuarioJpaEntity de(Usuario usuario) {
        UsuarioJpaEntity e = new UsuarioJpaEntity();
        e.id = usuario.getId();
        e.nome = usuario.getNome();
        e.email = usuario.getEmail();
        e.senhaHash = usuario.getSenhaHash();
        e.perfil = usuario.getPerfil();
        e.criadoEm = usuario.getCriadoEm();
        return e;
    }

    Usuario paraDominio() {
        return Usuario.reconstituir(id, nome, email, senhaHash, perfil, criadoEm);
    }
}

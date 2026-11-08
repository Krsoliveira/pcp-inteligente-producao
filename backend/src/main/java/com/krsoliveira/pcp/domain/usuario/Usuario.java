package com.krsoliveira.pcp.domain.usuario;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

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
    private String senhaHash;
    private UUID perfilId;
    private boolean deveTrocarSenha;
    private final Instant criadoEm;
    private Instant senhaAlteradaEm;
    /** Desativado não entra nem usa sessão aberta, mas continua na trilha de auditoria e nas assinaturas. */
    private boolean ativo;

    private Usuario(UUID id, String nome, String email, String senhaHash,
                    UUID perfilId, boolean deveTrocarSenha, Instant criadoEm, Instant senhaAlteradaEm, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfilId = perfilId;
        this.deveTrocarSenha = deveTrocarSenha;
        this.criadoEm = criadoEm;
        this.senhaAlteradaEm = senhaAlteradaEm;
        this.ativo = ativo;
    }

    /**
     * Cria um novo usuário. A senha já deve chegar codificada (BCrypt);
     * a codificação é responsabilidade do caso de uso via {@link CodificadorDeSenha}.
     */
    public static Usuario criar(String nome, String email, String senhaHash, UUID perfilId) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail é obrigatório.");
        }
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }
        if (perfilId == null) {
            throw new IllegalArgumentException("Perfil é obrigatório.");
        }
        return new Usuario(UUID.randomUUID(), nome.trim(), email.trim().toLowerCase(),
                senhaHash, perfilId, false, Instant.now(), null, true);
    }

    /**
     * Usuário criado por um gestor: a senha é provisória (o gestor a conhece), então a troca no primeiro
     * acesso é obrigatória (ADR-0021).
     */
    public static Usuario criarComSenhaProvisoria(String nome, String email, String senhaHash, UUID perfilId) {
        Usuario usuario = criar(nome, email, senhaHash, perfilId);
        usuario.deveTrocarSenha = true;
        return usuario;
    }

    /**
     * Reconstrói um usuário existente a partir do banco de dados.
     * Não revalida invariantes: os dados já passaram por {@link #criar}.
     */
    public static Usuario reconstituir(UUID id, String nome, String email, String senhaHash,
                                       UUID perfilId, boolean deveTrocarSenha, Instant criadoEm, Instant senhaAlteradaEm,
                                       boolean ativo) {
        return new Usuario(id, nome, email, senhaHash, perfilId, deveTrocarSenha, criadoEm, senhaAlteradaEm, ativo);
    }

    /** Tira o acesso sem apagar o usuário. Quem pode, e se é o último Administrador, é decidido pelo caso de uso. */
    public void desativar() {
        if (!ativo) {
            throw new RegraDeNegocioException("O usuário %s já está desativado.".formatted(email));
        }
        this.ativo = false;
    }

    /** Devolve o acesso, com o mesmo perfil e a mesma senha de antes. */
    public void reativar() {
        if (ativo) {
            throw new RegraDeNegocioException("O usuário %s já está ativo.".formatted(email));
        }
        this.ativo = true;
    }

    /** O próprio usuário definiu uma senha nova: a troca obrigatória, se havia, deixa de valer. */
    public void trocarSenha(String novoHash) {
        exigirHash(novoHash);
        this.senhaHash = novoHash;
        this.deveTrocarSenha = false;
        this.senhaAlteradaEm = Instant.now();
    }

    /** Um gestor definiu uma senha provisória nova: o usuário precisa trocá-la no próximo acesso. */
    public void redefinirSenha(String novoHash) {
        exigirHash(novoHash);
        this.senhaHash = novoHash;
        this.deveTrocarSenha = true;
        this.senhaAlteradaEm = Instant.now();
    }

    private static void exigirHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }
    }

    /** Muda o perfil de acesso do usuário. Quem pode fazer isso é decidido pelo caso de uso. */
    public void atribuirPerfil(UUID novoPerfilId) {
        if (novoPerfilId == null) {
            throw new IllegalArgumentException("Perfil é obrigatório.");
        }
        this.perfilId = novoPerfilId;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public UUID getPerfilId() { return perfilId; }
    public boolean isDeveTrocarSenha() { return deveTrocarSenha; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    /** Última mudança de senha; nula se não mudou desde que isso passou a ser guardado. Token emitido antes não vale. */
    public Instant getSenhaAlteradaEm() { return senhaAlteradaEm; }
}

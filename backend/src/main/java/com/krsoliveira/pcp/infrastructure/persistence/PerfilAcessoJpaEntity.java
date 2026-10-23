package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Espelho das tabelas {@code perfil_acesso} e {@code perfil_acesso_permissao} para o JPA.
 * As permissões vêm junto (EAGER): o perfil sem elas não faz sentido, e são poucas.
 */
@Entity
@Table(name = "perfil_acesso")
class PerfilAcessoJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false)
    private boolean administrador;

    @Column(name = "padrao_do_autocadastro", nullable = false)
    private boolean padraoDoAutocadastro;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_acesso_permissao", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "permissao", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Set<Permissao> permissoes = new HashSet<>();

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "criado_por", nullable = false, length = 150)
    private String criadoPor;

    @Column(name = "atualizado_por", nullable = false, length = 150)
    private String atualizadoPor;

    protected PerfilAcessoJpaEntity() {}

    static PerfilAcessoJpaEntity de(PerfilAcesso perfil) {
        PerfilAcessoJpaEntity e = new PerfilAcessoJpaEntity();
        e.id = perfil.getId();
        e.nome = perfil.getNome();
        e.descricao = perfil.getDescricao();
        e.administrador = perfil.isAdministrador();
        e.padraoDoAutocadastro = perfil.isPadraoDoAutocadastro();
        // O Administrador não grava linhas: tem todas as permissões por definição.
        e.permissoes = perfil.isAdministrador() ? new HashSet<>() : new HashSet<>(perfil.permissoesEfetivas());
        Assinatura a = perfil.getAssinatura();
        e.criadoEm = a.criadoEm();
        e.atualizadoEm = a.alteradoEm();
        e.criadoPor = a.criadoPor();
        e.atualizadoPor = a.alteradoPor();
        return e;
    }

    PerfilAcesso paraDominio() {
        return PerfilAcesso.reconstituir(id, nome, descricao, administrador, padraoDoAutocadastro, permissoes,
                new Assinatura(criadoPor, criadoEm, atualizadoPor, atualizadoEm));
    }
}

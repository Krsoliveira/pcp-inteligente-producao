package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Espelho da tabela {@code lista_tecnica} para o JPA/Hibernate.
 * Carrega os itens eagerly — a lista técnica sem componentes não faz sentido.
 */
@Entity
@Table(name = "lista_tecnica")
public class ListaTecnicaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(nullable = false, length = 50)
    private String versao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusListaTecnica status;

    @OneToMany(mappedBy = "listaTecnica", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemListaTecnicaJpaEntity> itens = new ArrayList<>();

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected ListaTecnicaJpaEntity() {}

    public static ListaTecnicaJpaEntity deDominio(ListaTecnica lista) {
        ListaTecnicaJpaEntity entity = new ListaTecnicaJpaEntity();
        entity.id = lista.getId();
        entity.materialId = lista.getMaterialId();
        entity.versao = lista.getVersao();
        entity.status = lista.getStatus();
        entity.criadaEm = lista.getCriadaEm();
        entity.atualizadaEm = lista.getAtualizadaEm();

        entity.itens = lista.getItens().stream()
                .map(item -> ItemListaTecnicaJpaEntity.deDominio(item, entity))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        return entity;
    }

    public ListaTecnica paraDominio() {
        List<ItemListaTecnica> itensDominio = itens.stream()
                .map(ItemListaTecnicaJpaEntity::paraDominio)
                .toList();
        return ListaTecnica.reconstituir(id, materialId, versao, status,
                itensDominio, criadaEm, atualizadaEm);
    }

    public UUID getId() { return id; }
    public StatusListaTecnica getStatus() { return status; }
    public void setStatus(StatusListaTecnica status) { this.status = status; }
    public void setAtualizadaEm(Instant atualizadaEm) { this.atualizadaEm = atualizadaEm; }
}

package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidade central do domínio: a autorização para fabricar uma quantidade
 * de um produto dentro de um período planejado.
 *
 * Classe de domínio PURA: sem anotações de framework (Spring, JPA).
 * Toda mudança de estado passa por métodos que validam as regras de negócio —
 * é impossível construir ou levar uma ordem a um estado inválido.
 */
public class OrdemProducao {

    private final UUID id;
    private final String codigo;
    private final String produto;
    private final int quantidade;
    private final LocalDate inicioPlanejado;
    private final LocalDate fimPlanejado;
    private StatusOrdemProducao status;
    private final Instant criadaEm;
    private Instant atualizadaEm;

    private OrdemProducao(UUID id, String codigo, String produto, int quantidade,
                          LocalDate inicioPlanejado, LocalDate fimPlanejado,
                          StatusOrdemProducao status, Instant criadaEm, Instant atualizadaEm) {
        this.id = id;
        this.codigo = codigo;
        this.produto = produto;
        this.quantidade = quantidade;
        this.inicioPlanejado = inicioPlanejado;
        this.fimPlanejado = fimPlanejado;
        this.status = status;
        this.criadaEm = criadaEm;
        this.atualizadaEm = atualizadaEm;
    }

    /**
     * Fábrica para uma ordem NOVA. Valida todas as invariantes antes de criar —
     * se retornar, a ordem é garantidamente válida e nasce PLANEJADA.
     */
    public static OrdemProducao criar(String codigo, String produto, int quantidade,
                                      LocalDate inicioPlanejado, LocalDate fimPlanejado) {
        if (codigo == null || codigo.isBlank()) {
            throw new RegraDeNegocioException("O código da ordem é obrigatório.");
        }
        if (produto == null || produto.isBlank()) {
            throw new RegraDeNegocioException("O produto da ordem é obrigatório.");
        }
        if (quantidade <= 0) {
            throw new RegraDeNegocioException("A quantidade deve ser maior que zero.");
        }
        if (inicioPlanejado == null || fimPlanejado == null) {
            throw new RegraDeNegocioException("As datas planejadas de início e fim são obrigatórias.");
        }
        if (fimPlanejado.isBefore(inicioPlanejado)) {
            throw new RegraDeNegocioException(
                    "A data de fim planejada não pode ser anterior à de início.");
        }
        Instant agora = Instant.now();
        return new OrdemProducao(UUID.randomUUID(), codigo.trim(), produto.trim(), quantidade,
                inicioPlanejado, fimPlanejado, StatusOrdemProducao.PLANEJADA, agora, agora);
    }

    /**
     * Reconstrói uma ordem EXISTENTE a partir do banco de dados.
     * Não revalida invariantes: os dados persistidos já passaram por {@link #criar}.
     */
    public static OrdemProducao reconstituir(UUID id, String codigo, String produto, int quantidade,
                                             LocalDate inicioPlanejado, LocalDate fimPlanejado,
                                             StatusOrdemProducao status,
                                             Instant criadaEm, Instant atualizadaEm) {
        return new OrdemProducao(id, codigo, produto, quantidade,
                inicioPlanejado, fimPlanejado, status, criadaEm, atualizadaEm);
    }

    /**
     * Avança (ou cancela) o ciclo de vida da ordem, respeitando a máquina
     * de estados de {@link StatusOrdemProducao}.
     */
    public void alterarStatusPara(StatusOrdemProducao novoStatus) {
        if (!status.podeTransicionarPara(novoStatus)) {
            throw new RegraDeNegocioException(
                    "Transição de status inválida: %s -> %s.".formatted(status, novoStatus));
        }
        this.status = novoStatus;
        this.atualizadaEm = Instant.now();
    }

    /**
     * Uma ordem está atrasada se continua aberta depois da data de fim planejada.
     * A data de referência é parâmetro (e não {@code LocalDate.now()}) para o
     * método ser determinístico e testável.
     */
    public boolean estaAtrasada(LocalDate dataDeReferencia) {
        return status.estaAberta() && dataDeReferencia.isAfter(fimPlanejado);
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public LocalDate getInicioPlanejado() {
        return inicioPlanejado;
    }

    public LocalDate getFimPlanejado() {
        return fimPlanejado;
    }

    public StatusOrdemProducao getStatus() {
        return status;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }
}

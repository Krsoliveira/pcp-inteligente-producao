package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidade central do domínio: a autorização para fabricar uma quantidade
 * de um material, segundo uma lista técnica específica, dentro de um período
 * planejado, num centro de trabalho.
 *
 * Campos substituídos na Fase 5a (ADR-0007):
 *   - {@code produto} (VARCHAR) → {@code materialId} + {@code listaTecnicaId} (FKs)
 *
 * Campos adicionados na Fase 5b:
 *   - {@code quantidadeProduzida} — preenchido ao concluir a ordem
 *   - {@code tipoOrdemId} — categorização opcional definida pelo usuário
 *
 * Classe de domínio PURA: sem anotações de framework (Spring, JPA).
 * Toda mudança de estado passa por métodos que validam as regras de negócio —
 * é impossível construir ou levar uma ordem a um estado inválido.
 */
public class OrdemProducao {

    private final UUID id;
    private final String codigo;
    private final UUID materialId;
    private final UUID listaTecnicaId;
    private final UUID tipoOrdemId;
    private final String centroDeTrabalho;
    private final int quantidade;
    private BigDecimal quantidadeProduzida;
    private final LocalDate inicioPlanejado;
    private final LocalDate fimPlanejado;
    private StatusOrdemProducao status;
    private final Instant criadaEm;
    private Instant atualizadaEm;

    private OrdemProducao(UUID id, String codigo, UUID materialId, UUID listaTecnicaId,
                          UUID tipoOrdemId, String centroDeTrabalho, int quantidade,
                          BigDecimal quantidadeProduzida,
                          LocalDate inicioPlanejado, LocalDate fimPlanejado,
                          StatusOrdemProducao status, Instant criadaEm, Instant atualizadaEm) {
        this.id = id;
        this.codigo = codigo;
        this.materialId = materialId;
        this.listaTecnicaId = listaTecnicaId;
        this.tipoOrdemId = tipoOrdemId;
        this.centroDeTrabalho = centroDeTrabalho;
        this.quantidade = quantidade;
        this.quantidadeProduzida = quantidadeProduzida;
        this.inicioPlanejado = inicioPlanejado;
        this.fimPlanejado = fimPlanejado;
        this.status = status;
        this.criadaEm = criadaEm;
        this.atualizadaEm = atualizadaEm;
    }

    /**
     * Fábrica para uma ordem NOVA. Valida todas as invariantes antes de criar —
     * se retornar, a ordem é garantidamente válida e nasce PLANEJADA.
     *
     * @param tipoOrdemId categorização da ordem (opcional — pode ser {@code null})
     */
    public static OrdemProducao criar(String codigo, UUID materialId, UUID listaTecnicaId,
                                      UUID tipoOrdemId, String centroDeTrabalho, int quantidade,
                                      LocalDate inicioPlanejado, LocalDate fimPlanejado) {
        if (codigo == null || codigo.isBlank()) {
            throw new RegraDeNegocioException("O código da ordem é obrigatório.");
        }
        if (materialId == null) {
            throw new RegraDeNegocioException("O material da ordem é obrigatório.");
        }
        if (listaTecnicaId == null) {
            throw new RegraDeNegocioException("A lista técnica da ordem é obrigatória.");
        }
        if (centroDeTrabalho == null || centroDeTrabalho.isBlank()) {
            throw new RegraDeNegocioException("O centro de trabalho é obrigatório.");
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
        return new OrdemProducao(UUID.randomUUID(), codigo.trim(), materialId, listaTecnicaId,
                tipoOrdemId, centroDeTrabalho.trim(), quantidade, null,
                inicioPlanejado, fimPlanejado, StatusOrdemProducao.PLANEJADA, agora, agora);
    }

    /**
     * Reconstrói uma ordem EXISTENTE a partir do banco de dados ou de carga inicial.
     * Não revalida invariantes: os dados persistidos já passaram por {@link #criar}.
     */
    public static OrdemProducao reconstituir(UUID id, String codigo, UUID materialId,
                                             UUID listaTecnicaId, UUID tipoOrdemId,
                                             String centroDeTrabalho, int quantidade,
                                             BigDecimal quantidadeProduzida,
                                             LocalDate inicioPlanejado, LocalDate fimPlanejado,
                                             StatusOrdemProducao status,
                                             Instant criadaEm, Instant atualizadaEm) {
        return new OrdemProducao(id, codigo, materialId, listaTecnicaId, tipoOrdemId,
                centroDeTrabalho, quantidade, quantidadeProduzida,
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
     * Conclui a ordem de produção com a quantidade efetivamente produzida.
     * Só é possível a partir do status EM_PRODUCAO. A validação de consumos
     * e geração de lote é responsabilidade do caso de uso {@code ConcluirOrdemProducao}.
     */
    public void concluir(BigDecimal quantidadeProduzida) {
        if (!status.podeSerConcluida()) {
            throw new RegraDeNegocioException(
                    "Apenas ordens EM_PRODUCAO podem ser concluídas. Status atual: %s.".formatted(status));
        }
        if (quantidadeProduzida == null || quantidadeProduzida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("A quantidade produzida deve ser maior que zero.");
        }
        this.quantidadeProduzida = quantidadeProduzida;
        this.status = StatusOrdemProducao.CONCLUIDA;
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

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public UUID getMaterialId() { return materialId; }
    public UUID getListaTecnicaId() { return listaTecnicaId; }
    public UUID getTipoOrdemId() { return tipoOrdemId; }
    public String getCentroDeTrabalho() { return centroDeTrabalho; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getQuantidadeProduzida() { return quantidadeProduzida; }
    public LocalDate getInicioPlanejado() { return inicioPlanejado; }
    public LocalDate getFimPlanejado() { return fimPlanejado; }
    public StatusOrdemProducao getStatus() { return status; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}

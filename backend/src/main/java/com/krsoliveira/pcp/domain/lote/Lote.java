package com.krsoliveira.pcp.domain.lote;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Lote rastreável de material, com duas origens possíveis:
 * <ul>
 *   <li><b>Produção</b> — gerado ao concluir uma ordem ({@link #criar}).</li>
 *   <li><b>Compra</b> — entrada de matéria-prima recebida de fornecedor, com nota
 *       fiscal ({@link #receberCompra}).</li>
 * </ul>
 * Cada lote possui número rastreável, quantidade, <b>saldo</b>, datas de
 * fabricação/validade e status de ciclo de vida. O saldo diminui a cada
 * {@link #alocar alocação} a um consumo; ao zerar, o lote passa a CONSUMIDO.
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public class Lote {

    private static final DateTimeFormatter FORMATO_ANO_MES = DateTimeFormatter.ofPattern("yyyyMM");

    private final UUID id;
    private final String numeroLote;
    private final UUID materialId;
    private final UUID ordemProducaoId;
    private final OrigemCompra origemCompra;
    private final BigDecimal quantidade;
    private BigDecimal saldo;
    private final String unidadeDeMedida;
    private final LocalDate dataFabricacao;
    private final LocalDate dataValidade;
    private StatusLote status;
    private Assinatura assinatura;

    private Lote(UUID id, String numeroLote, UUID materialId, UUID ordemProducaoId,
                 OrigemCompra origemCompra, BigDecimal quantidade, BigDecimal saldo,
                 String unidadeDeMedida, LocalDate dataFabricacao, LocalDate dataValidade,
                 StatusLote status, Assinatura assinatura) {
        this.id = id;
        this.numeroLote = numeroLote;
        this.materialId = materialId;
        this.ordemProducaoId = ordemProducaoId;
        this.origemCompra = origemCompra;
        this.quantidade = quantidade;
        this.saldo = saldo;
        this.unidadeDeMedida = unidadeDeMedida;
        this.dataFabricacao = dataFabricacao;
        this.dataValidade = dataValidade;
        this.status = status;
        this.assinatura = assinatura;
    }

    /**
     * Fábrica para um lote de PRODUÇÃO. Valida todas as invariantes antes de criar —
     * se retornar, o lote é garantidamente válido e nasce DISPONIVEL.
     *
     * @param ordemProducaoId ordem que gerou o lote (para compras, use {@link #receberCompra})
     */
    public static Lote criar(String numeroLote, UUID materialId, UUID ordemProducaoId,
                             BigDecimal quantidade, String unidadeDeMedida,
                             LocalDate dataFabricacao, LocalDate dataValidade, String usuario) {
        validarDadosComuns(numeroLote, materialId, quantidade, unidadeDeMedida,
                dataFabricacao, dataValidade);
        return new Lote(UUID.randomUUID(), numeroLote.trim(), materialId, ordemProducaoId,
                null, quantidade, quantidade, unidadeDeMedida.trim(), dataFabricacao, dataValidade,
                StatusLote.DISPONIVEL, Assinatura.nova(usuario));
    }

    /**
     * Fábrica para um lote COMPRADO (entrada de matéria-prima). A {@link OrigemCompra}
     * — fornecedor, nota fiscal e datas de emissão e recebimento — é a rastreabilidade de
     * origem do material. A regra "somente matéria-prima" é validada pelo caso de uso,
     * que conhece o Material.
     */
    public static Lote receberCompra(String numeroLote, UUID materialId, OrigemCompra origemCompra,
                                     BigDecimal quantidade, String unidadeDeMedida,
                                     LocalDate dataFabricacao, LocalDate dataValidade, String usuario) {
        validarDadosComuns(numeroLote, materialId, quantidade, unidadeDeMedida,
                dataFabricacao, dataValidade);
        if (origemCompra == null) {
            throw new RegraDeNegocioException("A origem da compra (fornecedor e nota fiscal) é obrigatória.");
        }
        if (dataFabricacao.isAfter(origemCompra.dataRecebimento())) {
            throw new RegraDeNegocioException(
                    "A data de fabricação não pode ser posterior ao recebimento do material.");
        }
        return new Lote(UUID.randomUUID(), numeroLote.trim(), materialId, null, origemCompra,
                quantidade, quantidade, unidadeDeMedida.trim(), dataFabricacao, dataValidade,
                StatusLote.DISPONIVEL, Assinatura.nova(usuario));
    }

    private static void validarDadosComuns(String numeroLote, UUID materialId,
                                           BigDecimal quantidade, String unidadeDeMedida,
                                           LocalDate dataFabricacao, LocalDate dataValidade) {
        if (numeroLote == null || numeroLote.isBlank()) {
            throw new RegraDeNegocioException("O número do lote é obrigatório.");
        }
        if (materialId == null) {
            throw new RegraDeNegocioException("O material do lote é obrigatório.");
        }
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("A quantidade do lote deve ser maior que zero.");
        }
        if (unidadeDeMedida == null || unidadeDeMedida.isBlank()) {
            throw new RegraDeNegocioException("A unidade de medida do lote é obrigatória.");
        }
        if (dataFabricacao == null) {
            throw new RegraDeNegocioException("A data de fabricação do lote é obrigatória.");
        }
        if (dataValidade == null) {
            throw new RegraDeNegocioException("A data de validade do lote é obrigatória.");
        }
        if (dataValidade.isBefore(dataFabricacao)) {
            throw new RegraDeNegocioException(
                    "A data de validade não pode ser anterior à data de fabricação.");
        }
    }

    /**
     * Prefixo do número de lote: {@code MAT-{codigoMaterial}-{yyyyMM}}.
     * O número completo acrescenta um sequencial por prefixo — ver {@link #numeroLote}.
     */
    public static String prefixoNumeroLote(String codigoMaterial, LocalDate dataFabricacao) {
        return "MAT-%s-%s".formatted(codigoMaterial, dataFabricacao.format(FORMATO_ANO_MES));
    }

    /**
     * Número rastreável do lote: {@code MAT-{codigoMaterial}-{yyyyMM}-{seq:03d}}.
     */
    public static String numeroLote(String prefixo, int sequencial) {
        return "%s-%03d".formatted(prefixo, sequencial);
    }

    /**
     * Reconstrói um lote EXISTENTE a partir do banco de dados.
     * Não revalida invariantes.
     */
    public static Lote reconstituir(UUID id, String numeroLote, UUID materialId,
                                    UUID ordemProducaoId, OrigemCompra origemCompra,
                                    BigDecimal quantidade, BigDecimal saldo, String unidadeDeMedida,
                                    LocalDate dataFabricacao, LocalDate dataValidade,
                                    StatusLote status, Assinatura assinatura) {
        return new Lote(id, numeroLote, materialId, ordemProducaoId, origemCompra,
                quantidade, saldo, unidadeDeMedida, dataFabricacao, dataValidade, status, assinatura);
    }

    /**
     * Retira {@code quantidade} deste lote para o {@code consumo} e devolve a alocação —
     * o registro de que o material do consumo veio daqui.
     *
     * Só aloca lote DISPONIVEL, do mesmo material e unidade do consumo, dentro da validade
     * na {@code data} do consumo e sem ultrapassar o saldo. Ao zerar o saldo, o lote passa
     * a CONSUMIDO.
     */
    public AlocacaoLote alocar(ConsumoMaterial consumo, BigDecimal quantidade, LocalDate data,
                               String usuario) {
        Assinatura alterada = assinatura.alterada(usuario);
        if (!materialId.equals(consumo.getMaterialId())) {
            throw new RegraDeNegocioException(
                    "O lote %s não é do material deste consumo.".formatted(numeroLote));
        }
        if (!unidadeDeMedida.equalsIgnoreCase(consumo.getUnidadeDeMedida())) {
            throw new RegraDeNegocioException("O lote %s está em %s e o consumo em %s."
                    .formatted(numeroLote, unidadeDeMedida, consumo.getUnidadeDeMedida()));
        }
        if (status != StatusLote.DISPONIVEL) {
            throw new RegraDeNegocioException(
                    "O lote %s não está disponível (status: %s).".formatted(numeroLote, status));
        }
        if (dataValidade.isBefore(data)) {
            throw new RegraDeNegocioException(
                    "O lote %s venceu em %s.".formatted(numeroLote, dataValidade));
        }
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("A quantidade alocada deve ser maior que zero.");
        }
        if (quantidade.compareTo(saldo) > 0) {
            throw new RegraDeNegocioException("Saldo insuficiente no lote %s: disponível %s, solicitado %s."
                    .formatted(numeroLote, saldo.stripTrailingZeros().toPlainString(),
                            quantidade.stripTrailingZeros().toPlainString()));
        }
        this.saldo = saldo.subtract(quantidade);
        if (saldo.signum() == 0) {
            this.status = StatusLote.CONSUMIDO;
        }
        this.assinatura = alterada;
        return AlocacaoLote.registrar(consumo.getId(), id, quantidade, usuario, alterada.alteradoEm());
    }

    /** O lote veio de compra (entrada de material) e não de uma ordem de produção? */
    public boolean ehDeCompra() {
        return origemCompra != null;
    }

    /**
     * Bloqueia o lote (ex.: retenção por qualidade).
     * Só é possível se estiver DISPONIVEL.
     */
    public void bloquear(String usuario) {
        Assinatura alterada = assinatura.alterada(usuario);
        if (this.status != StatusLote.DISPONIVEL) {
            throw new RegraDeNegocioException(
                    "Apenas lotes disponíveis podem ser bloqueados. Status atual: %s.".formatted(status));
        }
        this.status = StatusLote.BLOQUEADO;
        this.assinatura = alterada;
    }

    /**
     * Marca o lote como vencido. Chamado por processo agendado
     * quando {@code dataValidade < hoje}.
     */
    public void marcarComoVencido(String usuario) {
        Assinatura alterada = assinatura.alterada(usuario);
        if (this.status != StatusLote.DISPONIVEL && this.status != StatusLote.BLOQUEADO) {
            throw new RegraDeNegocioException(
                    "Lotes já consumidos ou vencidos não podem ser marcados como vencidos. Status atual: %s."
                            .formatted(status));
        }
        this.status = StatusLote.VENCIDO;
        this.assinatura = alterada;
    }

    /**
     * Marca o lote como consumido (utilizado como insumo de outra ordem).
     * Só é possível se estiver DISPONIVEL.
     */
    public void marcarComoConsumido(String usuario) {
        Assinatura alterada = assinatura.alterada(usuario);
        if (this.status != StatusLote.DISPONIVEL) {
            throw new RegraDeNegocioException(
                    "Apenas lotes disponíveis podem ser consumidos. Status atual: %s.".formatted(status));
        }
        this.status = StatusLote.CONSUMIDO;
        this.saldo = BigDecimal.ZERO;
        this.assinatura = alterada;
    }

    public UUID getId() { return id; }
    public String getNumeroLote() { return numeroLote; }
    public UUID getMaterialId() { return materialId; }
    public UUID getOrdemProducaoId() { return ordemProducaoId; }
    public OrigemCompra getOrigemCompra() { return origemCompra; }
    public String getFornecedor() { return origemCompra == null ? null : origemCompra.fornecedor(); }
    public String getNotaFiscal() { return origemCompra == null ? null : origemCompra.notaFiscal(); }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getSaldo() { return saldo; }
    public String getUnidadeDeMedida() { return unidadeDeMedida; }
    public LocalDate getDataFabricacao() { return dataFabricacao; }
    public LocalDate getDataValidade() { return dataValidade; }
    public StatusLote getStatus() { return status; }
    public Assinatura getAssinatura() { return assinatura; }
    public Instant getCriadoEm() { return assinatura.criadoEm(); }
}

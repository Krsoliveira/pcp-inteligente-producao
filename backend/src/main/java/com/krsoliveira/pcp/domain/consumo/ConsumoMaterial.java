package com.krsoliveira.pcp.domain.consumo;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Registra o consumo real de cada componente durante a execução de uma
 * ordem de produção. Projetado automaticamente a partir da lista técnica
 * ao criar a ordem; o consumo real é registrado pelo operador.
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public class ConsumoMaterial {

    private final UUID id;
    private final UUID ordemProducaoId;
    private final UUID materialId;
    private final BigDecimal quantidadePlanejada;
    private BigDecimal quantidadeConsumida;
    private final String unidadeDeMedida;
    private String justificativa;
    private String justificadoPor;
    private Instant justificadoEm;
    private Assinatura assinatura;

    private ConsumoMaterial(UUID id, UUID ordemProducaoId, UUID materialId,
                            BigDecimal quantidadePlanejada, BigDecimal quantidadeConsumida,
                            String unidadeDeMedida, String justificativa,
                            String justificadoPor, Instant justificadoEm,
                            Assinatura assinatura) {
        this.id = id;
        this.ordemProducaoId = ordemProducaoId;
        this.materialId = materialId;
        this.quantidadePlanejada = quantidadePlanejada;
        this.quantidadeConsumida = quantidadeConsumida;
        this.unidadeDeMedida = unidadeDeMedida;
        this.justificativa = justificativa;
        this.justificadoPor = justificadoPor;
        this.justificadoEm = justificadoEm;
        this.assinatura = assinatura;
    }

    /**
     * Fábrica para um consumo PROJETADO a partir da lista técnica.
     * A quantidade consumida fica {@code null} até o operador registrar.
     */
    public static ConsumoMaterial projetar(UUID ordemProducaoId, UUID materialId,
                                           BigDecimal quantidadePlanejada,
                                           String unidadeDeMedida, String usuario) {
        if (ordemProducaoId == null) {
            throw new RegraDeNegocioException("A ordem de produção do consumo é obrigatória.");
        }
        if (materialId == null) {
            throw new RegraDeNegocioException("O material do consumo é obrigatório.");
        }
        if (quantidadePlanejada == null || quantidadePlanejada.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException(
                    "A quantidade planejada do consumo deve ser maior que zero.");
        }
        if (unidadeDeMedida == null || unidadeDeMedida.isBlank()) {
            throw new RegraDeNegocioException("A unidade de medida do consumo é obrigatória.");
        }
        return new ConsumoMaterial(UUID.randomUUID(), ordemProducaoId, materialId,
                quantidadePlanejada, null, unidadeDeMedida.trim(),
                null, null, null, Assinatura.nova(usuario));
    }

    /**
     * Reconstrói um consumo EXISTENTE a partir do banco de dados.
     */
    public static ConsumoMaterial reconstituir(UUID id, UUID ordemProducaoId, UUID materialId,
                                               BigDecimal quantidadePlanejada,
                                               BigDecimal quantidadeConsumida,
                                               String unidadeDeMedida,
                                               String justificativa, String justificadoPor,
                                               Instant justificadoEm, Assinatura assinatura) {
        return new ConsumoMaterial(id, ordemProducaoId, materialId, quantidadePlanejada,
                quantidadeConsumida, unidadeDeMedida, justificativa, justificadoPor,
                justificadoEm, assinatura);
    }

    /**
     * Registra o consumo real do material. Se houver desvio (consumida != planejada), a
     * justificativa é obrigatória, e o responsável por ela é o próprio usuário que
     * registra — não um nome digitado.
     */
    public void registrarConsumo(BigDecimal quantidadeConsumida, String justificativa, String usuario) {
        if (quantidadeConsumida == null || quantidadeConsumida.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraDeNegocioException(
                    "A quantidade consumida deve ser zero ou maior.");
        }

        BigDecimal desvio = quantidadeConsumida.subtract(this.quantidadePlanejada);
        boolean temDesvio = desvio.compareTo(BigDecimal.ZERO) != 0;

        if (temDesvio && (justificativa == null || justificativa.isBlank())) {
            throw new RegraDeNegocioException(
                    "Justificativa obrigatória quando há desvio entre quantidade consumida e planejada.");
        }

        Assinatura novaAssinatura = assinatura.alterada(usuario);
        this.quantidadeConsumida = quantidadeConsumida;
        if (temDesvio) {
            this.justificativa = justificativa.trim();
            this.justificadoPor = novaAssinatura.alteradoPor();
            this.justificadoEm = novaAssinatura.alteradoEm();
        } else {
            this.justificativa = null;
            this.justificadoPor = null;
            this.justificadoEm = null;
        }
        this.assinatura = novaAssinatura;
    }

    /**
     * Desvio: consumida − planejada. {@code null} se o consumo ainda não foi registrado.
     */
    public BigDecimal getDesvio() {
        if (quantidadeConsumida == null) return null;
        return quantidadeConsumida.subtract(quantidadePlanejada);
    }

    /**
     * O consumo foi registrado (quantidade consumida preenchida)?
     */
    public boolean estaRegistrado() {
        return quantidadeConsumida != null;
    }

    /**
     * O consumo está justificado? Verdadeiro se:
     * - o consumo ainda não foi registrado (não há o que justificar), OU
     * - não há desvio (consumida == planejada), OU
     * - há desvio e a justificativa está preenchida.
     */
    public boolean estaJustificado() {
        if (!estaRegistrado()) return true;
        BigDecimal desvio = getDesvio();
        if (desvio.compareTo(BigDecimal.ZERO) == 0) return true;
        return justificativa != null && !justificativa.isBlank();
    }

    public UUID getId() { return id; }
    public UUID getOrdemProducaoId() { return ordemProducaoId; }
    public UUID getMaterialId() { return materialId; }
    public BigDecimal getQuantidadePlanejada() { return quantidadePlanejada; }
    public BigDecimal getQuantidadeConsumida() { return quantidadeConsumida; }
    public String getUnidadeDeMedida() { return unidadeDeMedida; }
    public String getJustificativa() { return justificativa; }
    public String getJustificadoPor() { return justificadoPor; }
    public Instant getJustificadoEm() { return justificadoEm; }
    public Assinatura getAssinatura() { return assinatura; }
    public Instant getCriadoEm() { return assinatura.criadoEm(); }
}

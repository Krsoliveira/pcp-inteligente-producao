package com.krsoliveira.pcp.application.lote;

import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.material.TipoMaterial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Caso de uso: registrar a entrada (recebimento) de matéria-prima comprada.
 *
 * Orquestração:
 * 1. Valida que o material existe e é MATERIA_PRIMA.
 * 2. Rejeita a mesma nota fiscal do mesmo fornecedor para o mesmo material (duplicidade).
 * 3. Gera o número do lote com a mesma regra dos lotes de produção
 *    ({@code MAT-{codigo}-{yyyyMM}-{seq:03d}}).
 * 4. Cria e persiste o lote DISPONIVEL com fornecedor e nota fiscal.
 */
public class RegistrarEntradaMaterial {

    private final LoteRepository loteRepository;
    private final MaterialRepository materialRepository;

    public RegistrarEntradaMaterial(LoteRepository loteRepository,
                                    MaterialRepository materialRepository) {
        this.loteRepository = loteRepository;
        this.materialRepository = materialRepository;
    }

    public record Comando(UUID materialId,
                          String fornecedor,
                          String notaFiscal,
                          BigDecimal quantidade,
                          LocalDate dataFabricacao,
                          LocalDate dataValidade) {}

    public Lote executar(Comando comando) {
        Material material = materialRepository.buscarPorId(comando.materialId())
                .orElseThrow(() -> new MaterialNaoEncontradoException(comando.materialId()));

        if (material.getTipo() != TipoMaterial.MATERIA_PRIMA) {
            throw new RegraDeNegocioException(
                    "Somente matéria-prima pode dar entrada por compra. O material %s é %s."
                            .formatted(material.getCodigo(), material.getTipo()));
        }

        String fornecedor = comando.fornecedor() == null ? null : comando.fornecedor().trim();
        String notaFiscal = comando.notaFiscal() == null ? null : comando.notaFiscal().trim();
        if (fornecedor != null && notaFiscal != null
                && loteRepository.existeEntrada(material.getId(), fornecedor, notaFiscal)) {
            throw new EntradaMaterialDuplicadaException(notaFiscal, fornecedor);
        }

        String numeroLote = null;
        if (comando.dataFabricacao() != null) {
            String prefixo = Lote.prefixoNumeroLote(material.getCodigo(), comando.dataFabricacao());
            numeroLote = Lote.numeroLote(prefixo,
                    loteRepository.proximoSequencial(material.getId(), prefixo));
        }

        Lote lote = Lote.receberCompra(numeroLote, material.getId(), fornecedor, notaFiscal,
                comando.quantidade(), material.getUnidadeDeMedida(),
                comando.dataFabricacao(), comando.dataValidade());
        return loteRepository.salvar(lote);
    }
}

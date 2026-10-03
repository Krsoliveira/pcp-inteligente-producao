package com.krsoliveira.pcp.application.lote;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.lote.OrigemCompra;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.material.TipoMaterial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Caso de uso: registrar a entrada (recebimento) de matéria-prima comprada.
 *
 * Orquestração (atômica e auditada):
 * 1. Valida que o material existe e é MATERIA_PRIMA.
 * 2. Valida a origem da compra: fornecedor, nota fiscal, emissão e recebimento.
 * 3. Rejeita a mesma nota fiscal do mesmo fornecedor para o mesmo material (duplicidade).
 * 4. Gera o número do lote com a mesma regra dos lotes de produção
 *    ({@code MAT-{codigo}-{yyyyMM}-{seq:03d}}).
 * 5. Cria e persiste o lote DISPONIVEL e registra quem deu entrada.
 */
public class RegistrarEntradaMaterial {

    private final LoteRepository loteRepository;
    private final MaterialRepository materialRepository;
    private final ExecucaoAuditada execucao;

    public RegistrarEntradaMaterial(LoteRepository loteRepository,
                                    MaterialRepository materialRepository,
                                    ExecucaoAuditada execucao) {
        this.loteRepository = loteRepository;
        this.materialRepository = materialRepository;
        this.execucao = execucao;
    }

    public record Comando(UUID materialId,
                          String fornecedor,
                          String notaFiscal,
                          LocalDate dataEmissaoNf,
                          LocalDate dataRecebimento,
                          BigDecimal quantidade,
                          LocalDate dataFabricacao,
                          LocalDate dataValidade) {}

    public Lote executar(Comando comando) {
        return execucao.executar(ctx -> {
            Material material = materialRepository.buscarPorId(comando.materialId())
                    .orElseThrow(() -> new MaterialNaoEncontradoException(comando.materialId()));

            if (material.getTipo() != TipoMaterial.MATERIA_PRIMA) {
                throw new RegraDeNegocioException(
                        "Somente matéria-prima pode dar entrada por compra. O material %s é %s."
                                .formatted(material.getCodigo(), material.getTipo()));
            }

            OrigemCompra origem = new OrigemCompra(comando.fornecedor(), comando.notaFiscal(),
                    comando.dataEmissaoNf(), comando.dataRecebimento());

            if (loteRepository.existeEntrada(material.getId(), origem.fornecedor(), origem.notaFiscal())) {
                throw new EntradaMaterialDuplicadaException(origem.notaFiscal(), origem.fornecedor());
            }

            String numeroLote = null;
            if (comando.dataFabricacao() != null) {
                String prefixo = Lote.prefixoNumeroLote(material.getCodigo(), comando.dataFabricacao());
                numeroLote = Lote.numeroLote(prefixo,
                        loteRepository.proximoSequencial(material.getId(), prefixo));
            }

            Lote lote = loteRepository.salvar(Lote.receberCompra(numeroLote, material.getId(), origem,
                    comando.quantidade(), material.getUnidadeDeMedida(),
                    comando.dataFabricacao(), comando.dataValidade(), ctx.usuario()));

            ctx.registrar(TipoEntidade.LOTE, lote.getId(), lote.getNumeroLote(),
                    AcaoAuditoria.ENTRADA_REGISTRADA,
                    Detalhes.com("numeroLote", lote.getNumeroLote())
                            .e("material", material.getCodigo())
                            .e("fornecedor", origem.fornecedor())
                            .e("notaFiscal", origem.notaFiscal())
                            .e("dataEmissaoNf", origem.dataEmissaoNf())
                            .e("dataRecebimento", origem.dataRecebimento())
                            .e("quantidade", lote.getQuantidade())
                            .e("unidadeDeMedida", lote.getUnidadeDeMedida())
                            .e("dataFabricacao", lote.getDataFabricacao())
                            .e("dataValidade", lote.getDataValidade()));
            return lote;
        });
    }
}

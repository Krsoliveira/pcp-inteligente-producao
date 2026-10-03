package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Caso de uso: registrar o consumo real de um material numa ordem. O responsável é o
 * usuário logado; o evento entra no histórico da ordem com planejado, consumido e desvio.
 */
public class RegistrarConsumoMaterial {

    private final ConsumoMaterialRepository consumoRepository;
    private final OrdemProducaoRepository ordemRepository;
    private final MaterialRepository materialRepository;
    private final ExecucaoAuditada execucao;

    public RegistrarConsumoMaterial(ConsumoMaterialRepository consumoRepository,
                                    OrdemProducaoRepository ordemRepository,
                                    MaterialRepository materialRepository,
                                    ExecucaoAuditada execucao) {
        this.consumoRepository = consumoRepository;
        this.ordemRepository = ordemRepository;
        this.materialRepository = materialRepository;
        this.execucao = execucao;
    }

    public record Comando(UUID consumoMaterialId,
                          BigDecimal quantidadeConsumida,
                          String justificativa) {}

    public ConsumoMaterial executar(Comando comando) {
        return execucao.executar(ctx -> {
            ConsumoMaterial consumo = consumoRepository.buscarPorId(comando.consumoMaterialId())
                    .orElseThrow(() -> new ConsumoMaterialNaoEncontradoException(
                            comando.consumoMaterialId()));

            BigDecimal consumidaAntes = consumo.getQuantidadeConsumida();
            consumo.registrarConsumo(comando.quantidadeConsumida(), comando.justificativa(), ctx.usuario());
            ConsumoMaterial salvo = consumoRepository.salvar(consumo);

            String codigoOrdem = ordemRepository.buscarPorId(salvo.getOrdemProducaoId())
                    .map(OrdemProducao::getCodigo).orElse("");
            Detalhes detalhes = Detalhes
                    .com("material", materialRepository.buscarPorId(salvo.getMaterialId())
                            .map(Material::getCodigo).orElse(null))
                    .e("quantidadePlanejada", salvo.getQuantidadePlanejada())
                    .e("quantidadeConsumida", salvo.getQuantidadeConsumida())
                    .e("desvio", salvo.getDesvio())
                    .e("unidadeDeMedida", salvo.getUnidadeDeMedida())
                    .e("justificativa", salvo.getJustificativa());
            if (consumidaAntes != null) {
                detalhes.mudanca("quantidadeConsumida", consumidaAntes, salvo.getQuantidadeConsumida());
            }
            ctx.registrar(TipoEntidade.ORDEM_PRODUCAO, salvo.getOrdemProducaoId(), codigoOrdem,
                    AcaoAuditoria.CONSUMO_REGISTRADO, detalhes);
            return salvo;
        });
    }
}

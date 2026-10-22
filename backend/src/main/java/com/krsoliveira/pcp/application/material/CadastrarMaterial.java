package com.krsoliveira.pcp.application.material;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.material.TipoMaterial;

import java.util.UUID;

/**
 * Caso de uso: cadastrar um material (produto acabado, semiacabado ou matéria-prima).
 * O código é gerado automaticamente — o próximo livre na faixa do tipo (103, 105 ou 110,
 * ADR-0012). Registra quem cadastrou.
 */
public class CadastrarMaterial {

    private final MaterialRepository materialRepository;
    private final ExecucaoAuditada execucao;

    public CadastrarMaterial(MaterialRepository materialRepository, ExecucaoAuditada execucao) {
        this.materialRepository = materialRepository;
        this.execucao = execucao;
    }

    public record Comando(String descricao, TipoMaterial tipo, String unidadeDeMedida) {}

    public UUID executar(Comando comando) {
        return execucao.executar(ctx -> {
            if (comando.tipo() == null) {
                throw new RegraDeNegocioException("O tipo do material é obrigatório.");
            }
            String codigo = materialRepository.proximoCodigo(comando.tipo());
            Material material = Material.criar(codigo, comando.descricao(),
                    comando.tipo(), comando.unidadeDeMedida(), ctx.usuario());
            materialRepository.salvar(material);

            ctx.registrar(TipoEntidade.MATERIAL, material.getId(), material.getCodigo(),
                    AcaoAuditoria.CRIADO,
                    Detalhes.com("codigo", material.getCodigo())
                            .e("descricao", material.getDescricao())
                            .e("tipo", material.getTipo())
                            .e("unidadeDeMedida", material.getUnidadeDeMedida()));
            return material.getId();
        });
    }
}

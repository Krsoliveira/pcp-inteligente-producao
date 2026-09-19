package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.consumo.ProjetarConsumoMaterial;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Caso de uso: criar uma nova ordem de produção.
 *
 * Responsabilidades desta camada: verificar unicidade do código (regra que
 * exige consultar o repositório) e delegar as invariantes ao domínio.
 * Classe sem anotações Spring — é registrada como bean em
 * {@code infrastructure/config/ConfiguracaoCasosDeUso}.
 */
public class CriarOrdemProducao {

    private final OrdemProducaoRepository repositorio;
    private final ProjetarConsumoMaterial projetarConsumoMaterial;

    public CriarOrdemProducao(OrdemProducaoRepository repositorio,
                               ProjetarConsumoMaterial projetarConsumoMaterial) {
        this.repositorio = repositorio;
        this.projetarConsumoMaterial = projetarConsumoMaterial;
    }

    public OrdemProducao executar(Comando comando) {
        if (repositorio.existePorCodigo(comando.codigo())) {
            throw new CodigoJaUtilizadoException(comando.codigo());
        }
        OrdemProducao ordem = OrdemProducao.criar(
                comando.codigo(),
                comando.materialId(),
                comando.listaTecnicaId(),
                comando.tipoOrdemId(),
                comando.centroDeTrabalho(),
                comando.quantidade(),
                comando.inicioPlanejado(),
                comando.fimPlanejado());
        OrdemProducao ordemSalva = repositorio.salvar(ordem);

        projetarConsumoMaterial.executar(new ProjetarConsumoMaterial.Comando(
                ordemSalva.getId(),
                ordemSalva.getListaTecnicaId(),
                ordemSalva.getQuantidade()));

        return ordemSalva;
    }

    /**
     * Dados de entrada do caso de uso, desacoplados do formato HTTP.
     * @param tipoOrdemId categorização opcional da ordem (pode ser {@code null})
     */
    public record Comando(String codigo,
                          UUID materialId,
                          UUID listaTecnicaId,
                          UUID tipoOrdemId,
                          String centroDeTrabalho,
                          int quantidade,
                          LocalDate inicioPlanejado,
                          LocalDate fimPlanejado) {}
}

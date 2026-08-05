package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;

import java.time.LocalDate;

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

    public CriarOrdemProducao(OrdemProducaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public OrdemProducao executar(Comando comando) {
        if (repositorio.existePorCodigo(comando.codigo())) {
            throw new CodigoJaUtilizadoException(comando.codigo());
        }
        OrdemProducao ordem = OrdemProducao.criar(
                comando.codigo(),
                comando.produto(),
                comando.centroDeTrabalho(),
                comando.quantidade(),
                comando.inicioPlanejado(),
                comando.fimPlanejado());
        return repositorio.salvar(ordem);
    }

    /**
     * Dados de entrada do caso de uso, desacoplados do formato HTTP.
     */
    public record Comando(String codigo,
                          String produto,
                          String centroDeTrabalho,
                          int quantidade,
                          LocalDate inicioPlanejado,
                          LocalDate fimPlanejado) {
    }
}
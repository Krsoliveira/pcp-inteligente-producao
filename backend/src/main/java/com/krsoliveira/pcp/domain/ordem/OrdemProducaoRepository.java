package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * PORTA de persistência: o domínio declara O QUE precisa; a infraestrutura
 * decide COMO fazer (JPA/PostgreSQL em produção, memória nos testes).
 *
 * Repare: nenhuma menção a JPA, SQL ou Spring — só tipos do domínio e do Java.
 */
public interface OrdemProducaoRepository {

    OrdemProducao salvar(OrdemProducao ordem);

    Optional<OrdemProducao> buscarPorId(UUID id);

    List<OrdemProducao> buscarPorIds(Collection<UUID> ids);

    List<OrdemProducao> listarTodas();

    /**
     * Uma página de ordens que atendem ao filtro, das mais recentes (data de criação) para as
     * mais antigas; empates saem por código decrescente.
     */
    Pagina<OrdemProducao> pesquisar(FiltroOrdens filtro, PedidoDePagina pedido);

    /** Quantas ordens há em cada status; status sem ordens podem ficar de fora do mapa. */
    Map<StatusOrdemProducao, Long> contarPorStatus();

    /** Ordens abertas ({@link StatusOrdemProducao#estaAberta()}) cujo fim planejado já passou em {@code hoje}. */
    long contarAtrasadas(LocalDate hoje);

    /** Ordens cujo fim planejado está entre as datas, inclusive. */
    List<OrdemProducao> listarComFimPlanejadoEntre(LocalDate de, LocalDate ate);

    /**
     * Ordens abertas e atrasadas em {@code hoje}, agrupadas por centro de trabalho: do centro com mais ordens
     * atrasadas para o com menos (empate pelo nome do centro).
     */
    List<AtrasoPorCentro> atrasadasPorCentro(LocalDate hoje);

    boolean existePorCodigo(String codigo);

    long contarTodas();
}

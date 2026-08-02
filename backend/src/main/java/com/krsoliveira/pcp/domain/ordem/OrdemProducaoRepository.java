package com.krsoliveira.pcp.domain.ordem;

import java.util.List;
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

    List<OrdemProducao> listarTodas();

    boolean existePorCodigo(String codigo);
}

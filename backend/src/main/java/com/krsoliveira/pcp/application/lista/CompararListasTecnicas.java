package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.comum.PermissoesDoUsuarioAtual;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.lista.ComparacaoDeVersoes;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;

import java.util.UUID;

/**
 * Caso de uso: comparar duas versões da mesma lista técnica (ADR-0019). Só leitura.
 *
 * Autorização por objeto (ADR-0020): comparar não revela versões que o usuário não pode ver. Quem só
 * tem {@code LISTA_VISUALIZAR_ATIVAS} compara apenas versões ativas; uma versão em revisão ou obsoleta
 * é tratada como inexistente (404).
 */
public class CompararListasTecnicas {

    private final ListaTecnicaRepository listaTecnicaRepository;
    private final PermissoesDoUsuarioAtual permissoesAtuais;

    public CompararListasTecnicas(ListaTecnicaRepository listaTecnicaRepository, PermissoesDoUsuarioAtual permissoesAtuais) {
        this.listaTecnicaRepository = listaTecnicaRepository;
        this.permissoesAtuais = permissoesAtuais;
    }

    public ComparacaoDeVersoes executar(UUID versaoA, UUID versaoB) {
        return ComparacaoDeVersoes.de(buscar(versaoA), buscar(versaoB));
    }

    private ListaTecnica buscar(UUID id) {
        boolean veTodas = permissoesAtuais.permissoes().contains(Permissao.LISTA_VISUALIZAR_TODAS);
        return listaTecnicaRepository.buscarPorId(id)
                .filter(lista -> veTodas || lista.getStatus() == StatusListaTecnica.ATIVA)
                .orElseThrow(() -> new ListaTecnicaNaoEncontradaException(id));
    }
}

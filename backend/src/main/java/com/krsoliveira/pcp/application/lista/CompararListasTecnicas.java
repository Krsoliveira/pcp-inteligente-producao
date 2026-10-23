package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.domain.lista.ComparacaoDeVersoes;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;

import java.util.UUID;

/**
 * Caso de uso: comparar duas versões da mesma lista técnica (ADR-0019). Só leitura.
 * O acesso é liberado a qualquer usuário autenticado por ora; a segregação por perfil é pendência.
 */
public class CompararListasTecnicas {

    private final ListaTecnicaRepository listaTecnicaRepository;

    public CompararListasTecnicas(ListaTecnicaRepository listaTecnicaRepository) {
        this.listaTecnicaRepository = listaTecnicaRepository;
    }

    public ComparacaoDeVersoes executar(UUID versaoA, UUID versaoB) {
        return ComparacaoDeVersoes.de(buscar(versaoA), buscar(versaoB));
    }

    private ListaTecnica buscar(UUID id) {
        return listaTecnicaRepository.buscarPorId(id)
                .orElseThrow(() -> new ListaTecnicaNaoEncontradaException(id));
    }
}

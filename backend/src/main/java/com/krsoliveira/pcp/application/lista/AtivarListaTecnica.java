package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: ativar uma lista técnica em revisão.
 *
 * Ao ativar uma nova versão, a versão atualmente ATIVA para o mesmo material
 * é automaticamente obsoletada — garantindo que nunca haja mais de uma versão
 * ATIVA por material.
 */
public class AtivarListaTecnica {

    private final ListaTecnicaRepository listaTecnicaRepository;

    public AtivarListaTecnica(ListaTecnicaRepository listaTecnicaRepository) {
        this.listaTecnicaRepository = listaTecnicaRepository;
    }

    public void executar(UUID listaTecnicaId) {
        ListaTecnica lista = listaTecnicaRepository.buscarPorId(listaTecnicaId)
                .orElseThrow(() -> new ListaTecnicaNaoEncontradaException(listaTecnicaId));

        Optional<ListaTecnica> ativaAtual =
                listaTecnicaRepository.buscarAtivaParaMaterial(lista.getMaterialId());

        ativaAtual.ifPresent(ativa -> {
            ativa.obsoleter();
            listaTecnicaRepository.salvar(ativa);
        });

        lista.ativar();
        listaTecnicaRepository.salvar(lista);
    }
}
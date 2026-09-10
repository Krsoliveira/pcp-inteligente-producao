package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: consultar listas técnicas (individual ou por material).
 */
public class ConsultarListaTecnica {

    private final ListaTecnicaRepository listaTecnicaRepository;

    public ConsultarListaTecnica(ListaTecnicaRepository listaTecnicaRepository) {
        this.listaTecnicaRepository = listaTecnicaRepository;
    }

    public ListaTecnica buscarPorId(UUID id) {
        return listaTecnicaRepository.buscarPorId(id)
                .orElseThrow(() -> new ListaTecnicaNaoEncontradaException(id));
    }

    public List<ListaTecnica> listarPorMaterial(UUID materialId) {
        return listaTecnicaRepository.listarPorMaterial(materialId);
    }
}
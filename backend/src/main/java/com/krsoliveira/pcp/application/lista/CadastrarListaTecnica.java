package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: cadastrar uma nova lista técnica (BOM) para um material.
 *
 * Regras:
 * - O material deve existir e não pode ser MATERIA_PRIMA.
 * - A combinação (materialId, versao) deve ser única.
 * - A lista deve ter ao menos um componente.
 * - A lista nasce com status EM_REVISAO.
 */
public class CadastrarListaTecnica {

    private final ListaTecnicaRepository listaTecnicaRepository;
    private final MaterialRepository materialRepository;

    public CadastrarListaTecnica(ListaTecnicaRepository listaTecnicaRepository,
                                 MaterialRepository materialRepository) {
        this.listaTecnicaRepository = listaTecnicaRepository;
        this.materialRepository = materialRepository;
    }

    public record ItemComando(UUID materialComponenteId, BigDecimal quantidadePlanejada,
                              String unidadeDeMedida) {}

    public record Comando(UUID materialId, String versao, List<ItemComando> itens) {}

    public UUID executar(Comando comando) {
        Material material = materialRepository.buscarPorId(comando.materialId())
                .orElseThrow(() -> new MaterialNaoEncontradoException(comando.materialId()));

        if (!material.getTipo().podeTermListaTecnica()) {
            throw new RegraDeNegocioException(
                    "Matéria-prima não pode ter lista técnica. Material: %s (%s)."
                            .formatted(material.getCodigo(), material.getTipo()));
        }

        if (listaTecnicaRepository.existeVersaoParaMaterial(
                comando.materialId(), comando.versao())) {
            throw new VersaoListaTecnicaJaExisteException(comando.materialId(), comando.versao());
        }

        List<ItemListaTecnica> itens = comando.itens().stream()
                .map(i -> ItemListaTecnica.criar(
                        i.materialComponenteId(),
                        i.quantidadePlanejada(),
                        i.unidadeDeMedida()))
                .toList();

        ListaTecnica lista = ListaTecnica.criar(comando.materialId(), comando.versao(), itens);
        listaTecnicaRepository.salvar(lista);
        return lista.getId();
    }
}
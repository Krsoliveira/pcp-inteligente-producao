package com.krsoliveira.pcp.domain.lista;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Diferença entre duas versões da MESMA lista técnica (ADR-0019). Os componentes são casados
 * pelo material componente: o que só existe na versão B foi adicionado, o que só existe na A foi
 * removido, e o que existe nas duas é "alterado" (quantidade ou unidade diferentes) ou "igual".
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public record ComparacaoDeVersoes(ListaTecnica versaoA, ListaTecnica versaoB,
                                  List<ItemComparado> adicionados,
                                  List<ItemComparado> removidos,
                                  List<ItemComparado> alterados,
                                  List<ItemComparado> iguais) {

    /**
     * Um componente nas duas versões; o lado em que ele não existe fica com quantidade e unidade nulas.
     */
    public record ItemComparado(UUID materialComponenteId,
                                BigDecimal quantidadeA, String unidadeA,
                                BigDecimal quantidadeB, String unidadeB) {}

    public static ComparacaoDeVersoes de(ListaTecnica a, ListaTecnica b) {
        if (a.getId().equals(b.getId())) {
            throw new RegraDeNegocioException("Escolha duas versões diferentes para comparar.");
        }
        if (!a.getListaId().equals(b.getListaId())) {
            throw new RegraDeNegocioException("Só é possível comparar versões da mesma lista técnica.");
        }

        Map<UUID, ItemListaTecnica> itensA = porComponente(a);
        Map<UUID, ItemListaTecnica> itensB = porComponente(b);

        List<ItemComparado> adicionados = new ArrayList<>();
        List<ItemComparado> removidos = new ArrayList<>();
        List<ItemComparado> alterados = new ArrayList<>();
        List<ItemComparado> iguais = new ArrayList<>();

        itensA.forEach((componente, itemA) -> {
            ItemListaTecnica itemB = itensB.get(componente);
            if (itemB == null) {
                removidos.add(new ItemComparado(componente, itemA.getQuantidadePlanejada(),
                        itemA.getUnidadeDeMedida(), null, null));
                return;
            }
            var comparado = new ItemComparado(componente, itemA.getQuantidadePlanejada(),
                    itemA.getUnidadeDeMedida(), itemB.getQuantidadePlanejada(), itemB.getUnidadeDeMedida());
            boolean mudou = itemA.getQuantidadePlanejada().compareTo(itemB.getQuantidadePlanejada()) != 0
                    || !itemA.getUnidadeDeMedida().equals(itemB.getUnidadeDeMedida());
            (mudou ? alterados : iguais).add(comparado);
        });
        itensB.forEach((componente, itemB) -> {
            if (!itensA.containsKey(componente)) {
                adicionados.add(new ItemComparado(componente, null, null,
                        itemB.getQuantidadePlanejada(), itemB.getUnidadeDeMedida()));
            }
        });

        return new ComparacaoDeVersoes(a, b, List.copyOf(adicionados), List.copyOf(removidos),
                List.copyOf(alterados), List.copyOf(iguais));
    }

    /** Há alguma diferença entre as versões? */
    public boolean temDiferencas() {
        return !adicionados.isEmpty() || !removidos.isEmpty() || !alterados.isEmpty();
    }

    private static Map<UUID, ItemListaTecnica> porComponente(ListaTecnica lista) {
        Map<UUID, ItemListaTecnica> itens = new LinkedHashMap<>();
        lista.getItens().forEach(item -> itens.putIfAbsent(item.getMaterialComponenteId(), item));
        return itens;
    }
}

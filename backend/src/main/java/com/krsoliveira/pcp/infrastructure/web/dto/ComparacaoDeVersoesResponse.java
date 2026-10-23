package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.lista.ComparacaoDeVersoes;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Resultado da comparação entre duas versões da mesma lista técnica. Os componentes vêm só com o
 * id do material; o cliente resolve código e descrição.
 */
public record ComparacaoDeVersoesResponse(
        VersaoResumo versaoA,
        VersaoResumo versaoB,
        boolean temDiferencas,
        List<ItemComparadoResponse> adicionados,
        List<ItemComparadoResponse> removidos,
        List<ItemComparadoResponse> alterados,
        List<ItemComparadoResponse> iguais
) {
    public record VersaoResumo(UUID id, UUID listaId, UUID materialId, String versao, StatusListaTecnica status) {
        static VersaoResumo de(ListaTecnica lista) {
            return new VersaoResumo(lista.getId(), lista.getListaId(), lista.getMaterialId(),
                    lista.getVersao(), lista.getStatus());
        }
    }

    public record ItemComparadoResponse(UUID materialComponenteId,
                                        BigDecimal quantidadeA, String unidadeA,
                                        BigDecimal quantidadeB, String unidadeB) {
        static ItemComparadoResponse de(ComparacaoDeVersoes.ItemComparado item) {
            return new ItemComparadoResponse(item.materialComponenteId(), item.quantidadeA(), item.unidadeA(),
                    item.quantidadeB(), item.unidadeB());
        }
    }

    public static ComparacaoDeVersoesResponse de(ComparacaoDeVersoes comparacao) {
        return new ComparacaoDeVersoesResponse(
                VersaoResumo.de(comparacao.versaoA()),
                VersaoResumo.de(comparacao.versaoB()),
                comparacao.temDiferencas(),
                comparacao.adicionados().stream().map(ItemComparadoResponse::de).toList(),
                comparacao.removidos().stream().map(ItemComparadoResponse::de).toList(),
                comparacao.alterados().stream().map(ItemComparadoResponse::de).toList(),
                comparacao.iguais().stream().map(ItemComparadoResponse::de).toList());
    }
}

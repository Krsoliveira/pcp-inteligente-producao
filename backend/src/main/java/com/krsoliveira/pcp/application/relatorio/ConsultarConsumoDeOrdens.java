package com.krsoliveira.pcp.application.relatorio;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: relatório de consumo de ordens. Para as ordens CONCLUÍDAS cujo fim planejado está no período,
 * uma linha por ordem e componente com o consumo total (planejado, consumido e desvio). O detalhe por lote fica na
 * tela da ordem: aqui o objetivo é o consumo total por componente.
 */
public class ConsultarConsumoDeOrdens {

    public static final int MAXIMO_DE_DIAS = 366;
    public static final int MAXIMO_DE_LINHAS = 5000;

    private final OrdemProducaoRepository ordens;
    private final ConsumoMaterialRepository consumos;
    private final MaterialRepository materiais;

    public ConsultarConsumoDeOrdens(OrdemProducaoRepository ordens, ConsumoMaterialRepository consumos,
                                    MaterialRepository materiais) {
        this.ordens = ordens;
        this.consumos = consumos;
        this.materiais = materiais;
    }

    /** @param de início do período do fim planejado (inclusive); @param ate fim do período (inclusive) */
    public RelatorioConsumoDeOrdens executar(LocalDate de, LocalDate ate) {
        validar(de, ate);

        List<OrdemProducao> concluidas = ordens.listarComFimPlanejadoEntre(de, ate).stream()
                .filter(o -> o.getStatus() == StatusOrdemProducao.CONCLUIDA)
                .sorted(Comparator.comparing(OrdemProducao::getFimPlanejado).thenComparing(OrdemProducao::getCodigo))
                .toList();
        if (concluidas.isEmpty()) {
            return new RelatorioConsumoDeOrdens(de, ate, 0, List.of());
        }

        Map<UUID, List<ConsumoMaterial>> consumosPorOrdem = consumos
                .listarPorOrdensProducao(concluidas.stream().map(OrdemProducao::getId).toList()).stream()
                .collect(Collectors.groupingBy(ConsumoMaterial::getOrdemProducaoId));
        Map<UUID, Material> materiaisPorId = materiais.listarTodos().stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));

        List<LinhaDeConsumoDeOrdem> linhas = new ArrayList<>();
        long ordensComLinhas = 0;
        for (OrdemProducao ordem : concluidas) {
            List<ConsumoMaterial> doComponente = consumosPorOrdem.getOrDefault(ordem.getId(), List.of());
            if (doComponente.isEmpty()) continue;
            ordensComLinhas++;
            Material produto = materiaisPorId.get(ordem.getMaterialId());
            doComponente.stream()
                    .map(c -> linha(ordem, produto, c, materiaisPorId.get(c.getMaterialId())))
                    .sorted(Comparator.comparing(LinhaDeConsumoDeOrdem::componenteCodigo))
                    .forEach(linhas::add);
            if (linhas.size() > MAXIMO_DE_LINHAS) {
                throw new RegraDeNegocioException(
                        "O relatório passa de %d linhas; reduza o período.".formatted(MAXIMO_DE_LINHAS));
            }
        }
        return new RelatorioConsumoDeOrdens(de, ate, ordensComLinhas, List.copyOf(linhas));
    }

    private static LinhaDeConsumoDeOrdem linha(OrdemProducao ordem, Material produto, ConsumoMaterial consumo,
                                               Material componente) {
        return new LinhaDeConsumoDeOrdem(ordem.getId(), ordem.getCodigo(), ordem.getFimPlanejado(),
                produto == null ? "—" : produto.getCodigo(), produto == null ? "—" : produto.getDescricao(),
                componente == null ? "—" : componente.getCodigo(), componente == null ? "—" : componente.getDescricao(),
                consumo.getUnidadeDeMedida(), consumo.getQuantidadePlanejada(), consumo.getQuantidadeConsumida(),
                consumo.getDesvio());
    }

    private static void validar(LocalDate de, LocalDate ate) {
        if (de == null || ate == null) {
            throw new RegraDeNegocioException("Informe o início e o fim do período.");
        }
        if (ate.isBefore(de)) {
            throw new RegraDeNegocioException("O fim do período não pode ser anterior ao início.");
        }
        if (ChronoUnit.DAYS.between(de, ate) >= MAXIMO_DE_DIAS) {
            throw new RegraDeNegocioException(
                    "O período do relatório pode ter no máximo %d dias.".formatted(MAXIMO_DE_DIAS));
        }
    }
}

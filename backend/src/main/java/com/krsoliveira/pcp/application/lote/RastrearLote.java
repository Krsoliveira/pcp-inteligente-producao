package com.krsoliveira.pcp.application.lote;

import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.lote.AlocacaoLote;
import com.krsoliveira.pcp.domain.lote.AlocacaoLoteRepository;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: genealogia de um lote, um nível em cada direção (ADR-0011).
 * <ul>
 *   <li><b>Origens</b> — para um lote de produção: cada consumo da ordem que o gerou e de
 *       quais lotes saiu o material.</li>
 *   <li><b>Destinos</b> — onde o lote foi usado: em qual consumo, de qual ordem, e qual
 *       lote essa ordem gerou.</li>
 * </ul>
 * Navegando de lote em lote chega-se do produto acabado à nota fiscal da matéria-prima,
 * e vice-versa. As consultas são feitas em lote (sem N+1).
 */
public class RastrearLote {

    private final LoteRepository loteRepository;
    private final AlocacaoLoteRepository alocacaoRepository;
    private final ConsumoMaterialRepository consumoRepository;
    private final OrdemProducaoRepository ordemRepository;

    public RastrearLote(LoteRepository loteRepository, AlocacaoLoteRepository alocacaoRepository,
                        ConsumoMaterialRepository consumoRepository, OrdemProducaoRepository ordemRepository) {
        this.loteRepository = loteRepository;
        this.alocacaoRepository = alocacaoRepository;
        this.consumoRepository = consumoRepository;
        this.ordemRepository = ordemRepository;
    }

    /** Um lote de origem e quanto dele foi usado. */
    public record LoteUsado(Lote lote, AlocacaoLote alocacao) {}

    /** Um consumo da ordem que gerou o lote e os lotes de onde ele saiu. */
    public record Origem(ConsumoMaterial consumo, List<LoteUsado> lotes) {}

    /** Um uso do lote: a alocação, o consumo, a ordem e o lote que ela gerou (se concluída). */
    public record Destino(AlocacaoLote alocacao, ConsumoMaterial consumo, OrdemProducao ordem,
                          Optional<Lote> loteGerado) {}

    public record Resultado(Lote lote, List<Origem> origens, List<Destino> destinos) {}

    public Resultado executar(UUID loteId) {
        Lote lote = loteRepository.buscarPorId(loteId)
                .orElseThrow(() -> new LoteNaoEncontradoException(loteId));
        return new Resultado(lote, origens(lote), destinos(lote));
    }

    private List<Origem> origens(Lote lote) {
        if (lote.getOrdemProducaoId() == null) {
            return List.of();
        }
        List<ConsumoMaterial> consumos = consumoRepository.listarPorOrdemProducao(lote.getOrdemProducaoId());
        List<AlocacaoLote> alocacoes = alocacaoRepository.listarPorConsumos(
                consumos.stream().map(ConsumoMaterial::getId).toList());
        Map<UUID, Lote> lotes = porId(loteRepository.buscarPorIds(
                alocacoes.stream().map(AlocacaoLote::getLoteId).distinct().toList()), Lote::getId);
        Map<UUID, List<AlocacaoLote>> porConsumo = alocacoes.stream()
                .collect(Collectors.groupingBy(AlocacaoLote::getConsumoMaterialId));

        return consumos.stream()
                .map(c -> new Origem(c, porConsumo.getOrDefault(c.getId(), List.of()).stream()
                        .filter(a -> lotes.containsKey(a.getLoteId()))
                        .map(a -> new LoteUsado(lotes.get(a.getLoteId()), a))
                        .toList()))
                .toList();
    }

    private List<Destino> destinos(Lote lote) {
        List<AlocacaoLote> alocacoes = alocacaoRepository.listarPorLote(lote.getId());
        if (alocacoes.isEmpty()) {
            return List.of();
        }
        Map<UUID, ConsumoMaterial> consumos = porId(consumoRepository.buscarPorIds(
                alocacoes.stream().map(AlocacaoLote::getConsumoMaterialId).distinct().toList()),
                ConsumoMaterial::getId);
        List<UUID> ordemIds = consumos.values().stream()
                .map(ConsumoMaterial::getOrdemProducaoId).distinct().toList();
        Map<UUID, OrdemProducao> ordens = porId(ordemRepository.buscarPorIds(ordemIds), OrdemProducao::getId);
        Map<UUID, Lote> lotesGerados = porId(loteRepository.listarPorOrdens(ordemIds),
                Lote::getOrdemProducaoId);

        return alocacoes.stream()
                .filter(a -> consumos.containsKey(a.getConsumoMaterialId()))
                .map(a -> {
                    ConsumoMaterial consumo = consumos.get(a.getConsumoMaterialId());
                    UUID ordemId = consumo.getOrdemProducaoId();
                    return new Destino(a, consumo, ordens.get(ordemId),
                            Optional.ofNullable(lotesGerados.get(ordemId)));
                })
                .filter(d -> d.ordem() != null)
                .sorted(Comparator.comparing((Destino d) -> d.alocacao().getCriadoEm()).reversed())
                .toList();
    }

    private static <T> Map<UUID, T> porId(List<T> itens, Function<T, UUID> chave) {
        return itens.stream().collect(Collectors.toMap(chave, Function.identity(), (a, b) -> a));
    }
}

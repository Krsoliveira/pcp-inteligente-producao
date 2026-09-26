package com.krsoliveira.pcp.infrastructure.seed;

import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Carga inicial do dataset sintético (ADR-0009), ativada pelo perfil {@code seed}.
 *
 * Lê os CSVs de {@code classpath:dados/} — gerados por
 * {@code data/scripts/gerar_dataset_sintetico.py} — e persiste, nesta ordem:
 * materiais → tipos de ordem → listas técnicas → ordens → consumos → lotes.
 *
 * Decisões:
 * <ul>
 *   <li><b>Dados mestres</b> (material, tipo, BOM) passam pelas fábricas {@code criar()} e
 *       pelas transições de domínio ({@code ativar()}/{@code obsoleter()}): as invariantes
 *       são validadas como se tivessem sido cadastradas pela API.</li>
 *   <li><b>Histórico</b> (ordens, consumos, lotes) usa {@code reconstituir()}: são fatos
 *       passados com status arbitrário (ADR-0006). O gerador Python valida as mesmas
 *       invariantes antes de exportar.</li>
 *   <li><b>Ancoragem de datas</b>: todas as datas são deslocadas para que a
 *       {@code data_referencia} do dataset coincida com a data da carga — o dashboard
 *       sempre mostra um cenário "atual" (carteira aberta, atrasos, lotes a vencer).</li>
 *   <li><b>Idempotência</b>: se já houver ordens ou materiais, a carga não roda.</li>
 *   <li><b>Atomicidade</b>: tudo numa transação — falha no meio não deixa carga parcial.</li>
 * </ul>
 */
@Component
@Profile("seed")
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final String DIRETORIO = "dados/";

    private final MaterialRepository materialRepository;
    private final TipoOrdemRepository tipoOrdemRepository;
    private final ListaTecnicaRepository listaTecnicaRepository;
    private final OrdemProducaoRepository ordemRepository;
    private final ConsumoMaterialRepository consumoRepository;
    private final LoteRepository loteRepository;
    private final Clock relogio;

    @Autowired
    public DataLoader(MaterialRepository materialRepository,
                      TipoOrdemRepository tipoOrdemRepository,
                      ListaTecnicaRepository listaTecnicaRepository,
                      OrdemProducaoRepository ordemRepository,
                      ConsumoMaterialRepository consumoRepository,
                      LoteRepository loteRepository) {
        this(materialRepository, tipoOrdemRepository, listaTecnicaRepository, ordemRepository,
                consumoRepository, loteRepository, Clock.systemDefaultZone());
    }

    DataLoader(MaterialRepository materialRepository,
               TipoOrdemRepository tipoOrdemRepository,
               ListaTecnicaRepository listaTecnicaRepository,
               OrdemProducaoRepository ordemRepository,
               ConsumoMaterialRepository consumoRepository,
               LoteRepository loteRepository,
               Clock relogio) {
        this.materialRepository = materialRepository;
        this.tipoOrdemRepository = tipoOrdemRepository;
        this.listaTecnicaRepository = listaTecnicaRepository;
        this.ordemRepository = ordemRepository;
        this.consumoRepository = consumoRepository;
        this.loteRepository = loteRepository;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (ordemRepository.contarTodas() > 0 || !materialRepository.listarTodos().isEmpty()) {
            log.info("DataLoader: banco já possui dados — carga ignorada (idempotente).");
            return;
        }
        long inicio = System.currentTimeMillis();
        long deslocamentoDias = calcularDeslocamentoDias();

        Map<String, Material> materiais = carregarMateriais();
        Map<String, TipoOrdem> tipos = carregarTiposOrdem();
        Map<String, ListaTecnica> listas = carregarListasTecnicas(materiais);
        Map<String, OrdemProducao> ordens = carregarOrdens(materiais, tipos, listas, deslocamentoDias);
        int consumos = carregarConsumos(materiais, ordens);
        int lotes = carregarLotes(materiais, ordens, deslocamentoDias);

        log.info("DataLoader: carga concluída em {} ms — materiais={}, tiposOrdem={}, "
                        + "listasTecnicas={}, ordens={}, consumos={}, lotes={}, deslocamentoDias={}",
                System.currentTimeMillis() - inicio, materiais.size(), tipos.size(),
                listas.size(), ordens.size(), consumos, lotes, deslocamentoDias);
    }

    private long calcularDeslocamentoDias() {
        var propriedades = new Properties();
        try (InputStream in = new ClassPathResource(DIRETORIO + "dataset.properties").getInputStream()) {
            propriedades.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler dataset.properties", e);
        }
        LocalDate referencia = LocalDate.parse(obrigatorio(propriedades.getProperty("data_referencia"),
                "data_referencia em dataset.properties"));
        return ChronoUnit.DAYS.between(referencia, LocalDate.now(relogio));
    }

    private Map<String, Material> carregarMateriais() {
        Map<String, Material> materiais = new LinkedHashMap<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "materiais.csv")) {
            Material material = Material.criar(linha.get("codigo"), linha.get("descricao"),
                    TipoMaterial.valueOf(linha.get("tipo")), linha.get("unidade_de_medida"));
            materialRepository.salvar(material);
            materiais.put(material.getCodigo(), material);
        }
        return materiais;
    }

    private Map<String, TipoOrdem> carregarTiposOrdem() {
        Map<String, TipoOrdem> tipos = new LinkedHashMap<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "tipos_ordem.csv")) {
            TipoOrdem tipo = tipoOrdemRepository.salvar(
                    TipoOrdem.criar(linha.get("nome"), linha.get("descricao"), linha.get("cor")));
            tipos.put(tipo.getNome(), tipo);
        }
        return tipos;
    }

    private Map<String, ListaTecnica> carregarListasTecnicas(Map<String, Material> materiais) {
        Map<String, List<ItemListaTecnica>> itensPorLista = new HashMap<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "itens_lista_tecnica.csv")) {
            Material componente = buscar(materiais, linha.get("componente_codigo"), "material");
            itensPorLista.computeIfAbsent(chaveLista(linha.get("material_codigo"), linha.get("versao")),
                            k -> new ArrayList<>())
                    .add(ItemListaTecnica.criar(componente.getId(),
                            new BigDecimal(linha.get("quantidade_planejada")),
                            componente.getUnidadeDeMedida()));
        }

        Map<String, ListaTecnica> listas = new LinkedHashMap<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "listas_tecnicas.csv")) {
            Material material = buscar(materiais, linha.get("material_codigo"), "material");
            if (material.getTipo() == TipoMaterial.MATERIA_PRIMA) {
                throw new IllegalStateException(
                        "Matéria-prima não pode ter lista técnica: " + material.getCodigo());
            }
            String chave = chaveLista(material.getCodigo(), linha.get("versao"));
            ListaTecnica lista = ListaTecnica.criar(material.getId(), linha.get("versao"),
                    itensPorLista.getOrDefault(chave, List.of()));
            switch (linha.get("status")) {
                case "ATIVA" -> lista.ativar();
                case "OBSOLETA" -> {
                    lista.ativar();
                    lista.obsoleter();
                }
                case "EM_REVISAO" -> { /* estado inicial de criar() */ }
                default -> throw new IllegalStateException("Status de lista inválido: " + linha.get("status"));
            }
            listaTecnicaRepository.salvar(lista);
            listas.put(chave, lista);
        }
        return listas;
    }

    private Map<String, OrdemProducao> carregarOrdens(Map<String, Material> materiais,
                                                      Map<String, TipoOrdem> tipos,
                                                      Map<String, ListaTecnica> listas,
                                                      long deslocamentoDias) {
        Map<String, OrdemProducao> ordens = new LinkedHashMap<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "ordens_producao.csv")) {
            Material material = buscar(materiais, linha.get("material_codigo"), "material");
            ListaTecnica lista = buscar(listas, chaveLista(material.getCodigo(), linha.get("versao_lista")),
                    "lista técnica");
            TipoOrdem tipo = buscar(tipos, linha.get("tipo_ordem"), "tipo de ordem");
            LocalDate inicio = LocalDate.parse(linha.get("inicio_planejado")).plusDays(deslocamentoDias);
            LocalDate fim = LocalDate.parse(linha.get("fim_planejado")).plusDays(deslocamentoDias);
            String produzida = linha.get("quantidade_produzida");
            Instant criadaEm = instante(inicio);

            OrdemProducao ordem = OrdemProducao.reconstituir(UUID.randomUUID(), linha.get("codigo"),
                    material.getId(), lista.getId(), tipo.getId(), linha.get("centro_de_trabalho"),
                    Integer.parseInt(linha.get("quantidade")),
                    produzida.isBlank() ? null : new BigDecimal(produzida),
                    inicio, fim, StatusOrdemProducao.valueOf(linha.get("status")),
                    criadaEm, criadaEm);
            ordens.put(ordem.getCodigo(), ordemRepository.salvar(ordem));
        }
        return ordens;
    }

    private int carregarConsumos(Map<String, Material> materiais, Map<String, OrdemProducao> ordens) {
        List<ConsumoMaterial> consumos = new ArrayList<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "consumos_material.csv")) {
            OrdemProducao ordem = buscar(ordens, linha.get("ordem_codigo"), "ordem");
            Material componente = buscar(materiais, linha.get("componente_codigo"), "material");
            String consumida = linha.get("quantidade_consumida");
            String justificativa = linha.get("justificativa");
            boolean justificado = !justificativa.isBlank();

            consumos.add(ConsumoMaterial.reconstituir(UUID.randomUUID(), ordem.getId(),
                    componente.getId(), new BigDecimal(linha.get("quantidade_planejada")),
                    consumida.isBlank() ? null : new BigDecimal(consumida),
                    componente.getUnidadeDeMedida(),
                    justificado ? justificativa : null,
                    justificado ? linha.get("justificado_por") : null,
                    justificado ? instante(ordem.getFimPlanejado()) : null,
                    ordem.getCriadaEm()));
        }
        consumoRepository.salvarTodos(consumos);
        return consumos.size();
    }

    private int carregarLotes(Map<String, Material> materiais, Map<String, OrdemProducao> ordens,
                              long deslocamentoDias) {
        Map<UUID, Material> materiaisPorId = new HashMap<>();
        materiais.values().forEach(m -> materiaisPorId.put(m.getId(), m));
        // Banco vazio → o sequencial por prefixo é controlado em memória (evita 1 query por lote).
        Map<String, Integer> sequenciais = new HashMap<>();

        int total = 0;
        for (var linha : LeitorCsv.ler(DIRETORIO + "lotes.csv")) {
            OrdemProducao ordem = buscar(ordens, linha.get("ordem_codigo"), "ordem");
            if (ordem.getQuantidadeProduzida() == null) {
                throw new IllegalStateException("Lote para ordem sem quantidade produzida: " + ordem.getCodigo());
            }
            Material material = materiaisPorId.get(ordem.getMaterialId());
            LocalDate fabricacao = LocalDate.parse(linha.get("data_fabricacao")).plusDays(deslocamentoDias);
            LocalDate validade = LocalDate.parse(linha.get("data_validade")).plusDays(deslocamentoDias);
            String prefixo = Lote.prefixoNumeroLote(material.getCodigo(), fabricacao);
            int sequencial = sequenciais.merge(prefixo, 1, Integer::sum);

            loteRepository.salvar(Lote.reconstituir(UUID.randomUUID(),
                    Lote.numeroLote(prefixo, sequencial), material.getId(), ordem.getId(),
                    null, null, ordem.getQuantidadeProduzida(), material.getUnidadeDeMedida(),
                    fabricacao, validade, StatusLote.valueOf(linha.get("status")),
                    instante(fabricacao)));
            total++;
        }
        return total;
    }

    private Instant instante(LocalDate data) {
        return data.atTime(8, 0).atZone(relogio.getZone()).toInstant();
    }

    private static String chaveLista(String codigoMaterial, String versao) {
        return codigoMaterial + "|" + versao;
    }

    private static <T> T buscar(Map<String, T> mapa, String chave, String entidade) {
        T valor = mapa.get(chave);
        if (valor == null) {
            throw new IllegalStateException("Dataset inconsistente: %s '%s' não encontrado(a)."
                    .formatted(entidade, chave));
        }
        return valor;
    }

    private static String obrigatorio(String valor, String descricao) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Dataset inconsistente: " + descricao + " ausente.");
        }
        return valor.trim();
    }
}

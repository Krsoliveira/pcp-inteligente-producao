package com.krsoliveira.pcp.infrastructure.seed;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.auditoria.EventoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.auditoria.TrilhaDeAuditoria;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import com.krsoliveira.pcp.domain.lote.AlocacaoLote;
import com.krsoliveira.pcp.domain.lote.AlocacaoLoteRepository;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.lote.OrigemCompra;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntrada;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntradaRepository;
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
import java.time.LocalTime;
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
 * materiais → tipos de ordem → listas técnicas → ordens → consumos → lotes de produção →
 * notas fiscais de entrada com seus lotes de compra (ADR-0012) → alocações de lote (genealogia).
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
 *   <li><b>Rastreabilidade</b> (ADR-0011): todo registro é assinado por
 *       {@value #RESPONSAVEL} (consumos justificados, pelo responsável do dataset) e a
 *       trilha de auditoria recebe o histórico simulado — criação, transições de status,
 *       consumos, conclusões, notas fiscais, lotes e alocações —, com datas coerentes com o planejamento de cada
 *       ordem e nunca no futuro. Dados mestres são datados antes da primeira ordem.</li>
 *   <li><b>Idempotência</b>: se já houver ordens ou materiais, a carga não roda.</li>
 *   <li><b>Atomicidade</b>: tudo numa transação — falha no meio não deixa carga parcial.</li>
 * </ul>
 */
@Component
@Profile("seed")
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final String DIRETORIO = "dados/";
    static final String RESPONSAVEL = "sistema:carga-inicial";
    /** Antecedência do cadastro dos dados mestres em relação à primeira ordem. */
    private static final int DIAS_ANTES_DA_PRIMEIRA_ORDEM = 7;

    private final MaterialRepository materialRepository;
    private final TipoOrdemRepository tipoOrdemRepository;
    private final ListaTecnicaRepository listaTecnicaRepository;
    private final OrdemProducaoRepository ordemRepository;
    private final ConsumoMaterialRepository consumoRepository;
    private final LoteRepository loteRepository;
    private final AlocacaoLoteRepository alocacaoRepository;
    private final NotaFiscalEntradaRepository notaFiscalRepository;
    private final TrilhaDeAuditoria trilha;
    private final Clock relogio;
    private final List<EventoAuditoria> eventos = new ArrayList<>();
    private Instant agora;

    @Autowired
    public DataLoader(MaterialRepository materialRepository,
                      TipoOrdemRepository tipoOrdemRepository,
                      ListaTecnicaRepository listaTecnicaRepository,
                      OrdemProducaoRepository ordemRepository,
                      ConsumoMaterialRepository consumoRepository,
                      LoteRepository loteRepository,
                      AlocacaoLoteRepository alocacaoRepository,
                      NotaFiscalEntradaRepository notaFiscalRepository,
                      TrilhaDeAuditoria trilha) {
        this(materialRepository, tipoOrdemRepository, listaTecnicaRepository, ordemRepository,
                consumoRepository, loteRepository, alocacaoRepository, notaFiscalRepository, trilha,
                Clock.systemDefaultZone());
    }

    DataLoader(MaterialRepository materialRepository,
               TipoOrdemRepository tipoOrdemRepository,
               ListaTecnicaRepository listaTecnicaRepository,
               OrdemProducaoRepository ordemRepository,
               ConsumoMaterialRepository consumoRepository,
               LoteRepository loteRepository,
               AlocacaoLoteRepository alocacaoRepository,
               NotaFiscalEntradaRepository notaFiscalRepository,
               TrilhaDeAuditoria trilha,
               Clock relogio) {
        this.materialRepository = materialRepository;
        this.tipoOrdemRepository = tipoOrdemRepository;
        this.listaTecnicaRepository = listaTecnicaRepository;
        this.ordemRepository = ordemRepository;
        this.consumoRepository = consumoRepository;
        this.loteRepository = loteRepository;
        this.alocacaoRepository = alocacaoRepository;
        this.notaFiscalRepository = notaFiscalRepository;
        this.trilha = trilha;
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
        agora = Instant.now(relogio);
        eventos.clear();
        Instant cadastroMestre = momento(primeiroInicioPlanejado(deslocamentoDias)
                .minusDays(DIAS_ANTES_DA_PRIMEIRA_ORDEM), LocalTime.of(8, 0));

        Map<String, Material> materiais = carregarMateriais(cadastroMestre);
        Map<String, TipoOrdem> tipos = carregarTiposOrdem(cadastroMestre);
        Map<String, ListaTecnica> listas = carregarListasTecnicas(materiais, cadastroMestre);
        Map<String, OrdemProducao> ordens = carregarOrdens(materiais, tipos, listas, deslocamentoDias);
        Map<String, ConsumoCarga> consumos = carregarConsumos(materiais, ordens, deslocamentoDias);
        Map<String, LoteCarga> lotes = new LinkedHashMap<>();
        carregarLotesDeProducao(materiais, ordens, deslocamentoDias, lotes);
        int notas = carregarNotasFiscais(materiais, deslocamentoDias, lotes);
        List<AlocacaoLote> alocacoes = carregarAlocacoes(consumos, lotes);
        registrarConsumos(consumos);
        salvarLotes(lotes);
        alocacaoRepository.salvarTodas(alocacoes);
        trilha.registrar(eventos);

        log.info("DataLoader: carga concluída em {} ms — materiais={}, tiposOrdem={}, "
                        + "listasTecnicas={}, ordens={}, consumos={}, notasFiscais={}, lotes={}, alocacoes={}, "
                        + "eventosAuditoria={}, deslocamentoDias={}",
                System.currentTimeMillis() - inicio, materiais.size(), tipos.size(),
                listas.size(), ordens.size(), consumos.size(), notas, lotes.size(), alocacoes.size(),
                eventos.size(), deslocamentoDias);
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

    private LocalDate primeiroInicioPlanejado(long deslocamentoDias) {
        return LeitorCsv.ler(DIRETORIO + "ordens_producao.csv").stream()
                .map(linha -> LocalDate.parse(linha.get("inicio_planejado")))
                .min(LocalDate::compareTo)
                .map(data -> data.plusDays(deslocamentoDias))
                .orElse(LocalDate.now(relogio));
    }

    private Map<String, Material> carregarMateriais(Instant cadastradoEm) {
        Map<String, Material> materiais = new LinkedHashMap<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "materiais.csv")) {
            // criar() valida as invariantes; reconstituir() data o cadastro no passado.
            Material criado = Material.criar(linha.get("codigo"), linha.get("descricao"),
                    TipoMaterial.valueOf(linha.get("tipo")), linha.get("unidade_de_medida"), RESPONSAVEL);
            Material material = Material.reconstituir(criado.getId(), criado.getCodigo(),
                    criado.getDescricao(), criado.getTipo(), criado.getUnidadeDeMedida(),
                    assinatura(cadastradoEm, cadastradoEm));
            materialRepository.salvar(material);
            registrar(TipoEntidade.MATERIAL, material.getId(), material.getCodigo(), AcaoAuditoria.CRIADO,
                    RESPONSAVEL, cadastradoEm,
                    Detalhes.com("codigo", material.getCodigo())
                            .e("descricao", material.getDescricao())
                            .e("tipo", material.getTipo())
                            .e("unidadeDeMedida", material.getUnidadeDeMedida()));
            materiais.put(material.getCodigo(), material);
        }
        return materiais;
    }

    private Map<String, TipoOrdem> carregarTiposOrdem(Instant cadastradoEm) {
        Map<String, TipoOrdem> tipos = new LinkedHashMap<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "tipos_ordem.csv")) {
            TipoOrdem criado = TipoOrdem.criar(linha.get("nome"), linha.get("descricao"),
                    linha.get("cor"), RESPONSAVEL);
            TipoOrdem tipo = tipoOrdemRepository.salvar(TipoOrdem.reconstituir(criado.getId(),
                    criado.getNome(), criado.getDescricao(), criado.getCor(),
                    assinatura(cadastradoEm, cadastradoEm)));
            registrar(TipoEntidade.TIPO_ORDEM, tipo.getId(), tipo.getNome(), AcaoAuditoria.CRIADO,
                    RESPONSAVEL, cadastradoEm,
                    Detalhes.com("nome", tipo.getNome())
                            .e("descricao", tipo.getDescricao())
                            .e("cor", tipo.getCor()));
            tipos.put(tipo.getNome(), tipo);
        }
        return tipos;
    }

    private Map<String, ListaTecnica> carregarListasTecnicas(Map<String, Material> materiais,
                                                             Instant cadastradoEm) {
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
            ListaTecnica criada = ListaTecnica.criar(material.getId(), linha.get("versao"),
                    itensPorLista.getOrDefault(chave, List.of()), RESPONSAVEL);
            switch (linha.get("status")) {
                case "ATIVA" -> criada.ativar(RESPONSAVEL);
                case "OBSOLETA" -> {
                    criada.ativar(RESPONSAVEL);
                    criada.obsoleter(RESPONSAVEL);
                }
                case "EM_REVISAO" -> { /* estado inicial de criar() */ }
                default -> throw new IllegalStateException("Status de lista inválido: " + linha.get("status"));
            }
            ListaTecnica lista = ListaTecnica.reconstituir(criada.getId(), criada.getMaterialId(),
                    criada.getVersao(), criada.getStatus(), criada.getItens(),
                    assinatura(cadastradoEm, cadastradoEm));
            listaTecnicaRepository.salvar(lista);
            String referencia = material.getCodigo() + " " + lista.getVersao();
            registrar(TipoEntidade.LISTA_TECNICA, lista.getId(), referencia, AcaoAuditoria.CRIADO,
                    RESPONSAVEL, cadastradoEm,
                    Detalhes.com("material", material.getCodigo())
                            .e("versao", lista.getVersao())
                            .e("status", StatusListaTecnica.EM_REVISAO)
                            .e("itens", lista.getItens().size()));
            if (lista.getStatus() != StatusListaTecnica.EM_REVISAO) {
                registrar(TipoEntidade.LISTA_TECNICA, lista.getId(), referencia, AcaoAuditoria.ATIVADA,
                        RESPONSAVEL, cadastradoEm, Detalhes.com("versao", lista.getVersao()));
            }
            if (lista.getStatus() == StatusListaTecnica.OBSOLETA) {
                registrar(TipoEntidade.LISTA_TECNICA, lista.getId(), referencia, AcaoAuditoria.OBSOLETADA,
                        RESPONSAVEL, cadastradoEm, Detalhes.com("versao", lista.getVersao()));
            }
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
            StatusOrdemProducao status = StatusOrdemProducao.valueOf(linha.get("status"));
            UUID id = UUID.randomUUID();
            String codigo = linha.get("codigo");

            // Histórico simulado: criada 3 dias antes do início, liberada e iniciada no dia
            // do início, concluída no fim planejado (nunca depois de "agora").
            Instant criadaEm = momento(inicio.minusDays(3), LocalTime.of(8, 0));
            Instant ultimaAlteracao = criadaEm;
            registrar(TipoEntidade.ORDEM_PRODUCAO, id, codigo, AcaoAuditoria.CRIADO, RESPONSAVEL, criadaEm,
                    Detalhes.com("codigo", codigo)
                            .e("material", material.getCodigo())
                            .e("versaoListaTecnica", lista.getVersao())
                            .e("centroDeTrabalho", linha.get("centro_de_trabalho"))
                            .e("quantidade", Integer.parseInt(linha.get("quantidade")))
                            .e("inicioPlanejado", inicio)
                            .e("fimPlanejado", fim)
                            .e("status", StatusOrdemProducao.PLANEJADA)
                            .e("tipoOrdem", tipo.getNome()));
            for (var transicao : transicoesAte(status)) {
                Instant quando = posterior(ultimaAlteracao, transicao.para() == StatusOrdemProducao.CANCELADA
                        ? momento(inicio, LocalTime.of(10, 0))
                        : momento(inicio, transicao.para() == StatusOrdemProducao.LIBERADA
                                ? LocalTime.of(7, 0) : LocalTime.of(7, 30)));
                registrar(TipoEntidade.ORDEM_PRODUCAO, id, codigo, AcaoAuditoria.STATUS_ALTERADO,
                        RESPONSAVEL, quando, Detalhes.vazio().mudanca("status", transicao.de(), transicao.para()));
                ultimaAlteracao = quando;
            }
            if (status == StatusOrdemProducao.CONCLUIDA) {
                LocalDate conclusao = LocalDate.parse(linha.get("data_conclusao")).plusDays(deslocamentoDias);
                ultimaAlteracao = posterior(ultimaAlteracao, momento(conclusao, LocalTime.of(17, 0)));
            }

            OrdemProducao ordem = OrdemProducao.reconstituir(id, codigo,
                    material.getId(), lista.getId(), tipo.getId(), linha.get("centro_de_trabalho"),
                    Integer.parseInt(linha.get("quantidade")),
                    produzida.isBlank() ? null : new BigDecimal(produzida),
                    inicio, fim, status, assinatura(criadaEm, ultimaAlteracao));
            ordens.put(ordem.getCodigo(), ordemRepository.salvar(ordem));
        }
        return ordens;
    }

    private record Transicao(StatusOrdemProducao de, StatusOrdemProducao para) {}

    /** Transições registradas via STATUS_ALTERADO até o status final (a conclusão tem evento próprio). */
    private static List<Transicao> transicoesAte(StatusOrdemProducao status) {
        var planejadaLiberada = new Transicao(StatusOrdemProducao.PLANEJADA, StatusOrdemProducao.LIBERADA);
        var liberadaEmProducao = new Transicao(StatusOrdemProducao.LIBERADA, StatusOrdemProducao.EM_PRODUCAO);
        return switch (status) {
            case PLANEJADA -> List.of();
            case LIBERADA -> List.of(planejadaLiberada);
            case EM_PRODUCAO, CONCLUIDA -> List.of(planejadaLiberada, liberadaEmProducao);
            case CANCELADA -> List.of(new Transicao(StatusOrdemProducao.PLANEJADA, StatusOrdemProducao.CANCELADA));
        };
    }

    /** Consumo lido do dataset, com quem o registrou e quando (para alocações e eventos). */
    private record ConsumoCarga(ConsumoMaterial consumo, OrdemProducao ordem, Material componente,
                                String responsavel, Instant registradoEm, List<String> lotes) {}

    /** Lote em montagem: o saldo corre à medida que as alocações são aplicadas. */
    private static final class LoteCarga {
        final UUID id = UUID.randomUUID();
        final String numero;
        final Material material;
        final OrdemProducao ordem;
        final OrigemCompra origemCompra;
        final UUID notaFiscalId;
        final BigDecimal quantidade;
        final LocalDate fabricacao;
        final LocalDate validade;
        final StatusLote statusFinal;
        final BigDecimal saldoFinal;
        final Instant criadoEm;
        BigDecimal saldo;
        Instant alteradoEm;

        LoteCarga(String numero, Material material, OrdemProducao ordem, OrigemCompra origemCompra,
                  UUID notaFiscalId, BigDecimal quantidade, LocalDate fabricacao, LocalDate validade,
                  StatusLote statusFinal, BigDecimal saldoFinal, Instant criadoEm) {
            this.numero = numero;
            this.material = material;
            this.ordem = ordem;
            this.origemCompra = origemCompra;
            this.notaFiscalId = notaFiscalId;
            this.quantidade = quantidade;
            this.fabricacao = fabricacao;
            this.validade = validade;
            this.statusFinal = statusFinal;
            this.saldoFinal = saldoFinal;
            this.criadoEm = criadoEm;
            this.saldo = quantidade;
            this.alteradoEm = criadoEm;
        }
    }

    private Map<String, ConsumoCarga> carregarConsumos(Map<String, Material> materiais,
                                                       Map<String, OrdemProducao> ordens,
                                                       long deslocamentoDias) {
        Map<String, ConsumoCarga> consumos = new LinkedHashMap<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "consumos_material.csv")) {
            OrdemProducao ordem = buscar(ordens, linha.get("ordem_codigo"), "ordem");
            Material componente = buscar(materiais, linha.get("componente_codigo"), "material");
            String consumida = linha.get("quantidade_consumida");
            String justificativa = linha.get("justificativa");
            boolean justificado = !justificativa.isBlank();

            String responsavel = justificado ? linha.get("justificado_por") : RESPONSAVEL;
            Instant registradoEm = consumida.isBlank()
                    ? ordem.getCriadaEm()
                    : posterior(ordem.getCriadaEm(), momento(
                            LocalDate.parse(linha.get("data_registro")).plusDays(deslocamentoDias),
                            LocalTime.of(10, 0)));

            ConsumoMaterial consumo = ConsumoMaterial.reconstituir(UUID.randomUUID(), ordem.getId(),
                    componente.getId(), new BigDecimal(linha.get("quantidade_planejada")),
                    consumida.isBlank() ? null : new BigDecimal(consumida),
                    componente.getUnidadeDeMedida(),
                    justificado ? justificativa : null,
                    justificado ? responsavel : null,
                    justificado ? registradoEm : null,
                    new Assinatura(RESPONSAVEL, ordem.getCriadaEm(), responsavel, registradoEm));
            consumos.put(chaveConsumo(ordem.getCodigo(), componente.getCodigo()),
                    new ConsumoCarga(consumo, ordem, componente, responsavel, registradoEm, new ArrayList<>()));
        }
        consumoRepository.salvarTodos(consumos.values().stream().map(ConsumoCarga::consumo).toList());
        return consumos;
    }

    private void carregarLotesDeProducao(Map<String, Material> materiais, Map<String, OrdemProducao> ordens,
                                         long deslocamentoDias, Map<String, LoteCarga> lotes) {
        Map<UUID, Material> materiaisPorId = new HashMap<>();
        // Banco vazio → a sequência do dia (AAMMDD + NNNN) é controlada em memória.
        Map<LocalDate, Integer> sequenciaPorDia = new HashMap<>();
        materiais.values().forEach(m -> materiaisPorId.put(m.getId(), m));

        for (var linha : LeitorCsv.ler(DIRETORIO + "lotes.csv")) {
            OrdemProducao ordem = buscar(ordens, linha.get("ordem_codigo"), "ordem");
            if (ordem.getQuantidadeProduzida() == null) {
                throw new IllegalStateException("Lote para ordem sem quantidade produzida: " + ordem.getCodigo());
            }
            Material material = materiaisPorId.get(ordem.getMaterialId());
            LocalDate fabricacao = LocalDate.parse(linha.get("data_fabricacao")).plusDays(deslocamentoDias);
            LocalDate validade = LocalDate.parse(linha.get("data_validade")).plusDays(deslocamentoDias);
            String numero = Lote.numeroLoteProducao(fabricacao,
                    sequenciaPorDia.merge(fabricacao, 1, Integer::sum));
            Instant geradoEm = ordem.getAtualizadaEm();

            LoteCarga lote = new LoteCarga(numero, material, ordem, null, null, ordem.getQuantidadeProduzida(),
                    fabricacao, validade, StatusLote.valueOf(linha.get("status")),
                    new BigDecimal(linha.get("saldo")), geradoEm);
            lotes.put(refProducao(ordem.getCodigo()), lote);

            registrar(TipoEntidade.ORDEM_PRODUCAO, ordem.getId(), ordem.getCodigo(),
                    AcaoAuditoria.ORDEM_CONCLUIDA, RESPONSAVEL, geradoEm,
                    Detalhes.vazio()
                            .mudanca("status", StatusOrdemProducao.EM_PRODUCAO, StatusOrdemProducao.CONCLUIDA)
                            .e("quantidadePlanejada", ordem.getQuantidade())
                            .e("quantidadeProduzida", ordem.getQuantidadeProduzida())
                            .e("loteGerado", numero));
            registrar(TipoEntidade.LOTE, lote.id, numero, AcaoAuditoria.LOTE_GERADO, RESPONSAVEL, geradoEm,
                    Detalhes.com("numeroLote", numero)
                            .e("material", material.getCodigo())
                            .e("ordemProducao", ordem.getCodigo())
                            .e("quantidade", lote.quantidade)
                            .e("unidadeDeMedida", material.getUnidadeDeMedida())
                            .e("dataFabricacao", fabricacao)
                            .e("dataValidade", validade));
        }
    }

    /**
     * Cada nota do dataset vira um cabeçalho {@link NotaFiscalEntrada}; cada linha, um lote
     * de compra (item da nota) com o número de lote do fornecedor. Devolve quantas notas.
     */
    private int carregarNotasFiscais(Map<String, Material> materiais, long deslocamentoDias,
                                     Map<String, LoteCarga> lotes) {
        Map<String, List<Map<String, String>>> itensPorNota = new LinkedHashMap<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "lotes_compra.csv")) {
            itensPorNota.computeIfAbsent(linha.get("fornecedor").toLowerCase() + "|" + linha.get("nota_fiscal"),
                    k -> new ArrayList<>()).add(linha);
        }
        for (var itens : itensPorNota.values()) {
            var cabecalho = itens.get(0);
            OrigemCompra origem = new OrigemCompra(cabecalho.get("fornecedor"), cabecalho.get("nota_fiscal"),
                    LocalDate.parse(cabecalho.get("data_emissao_nf")).plusDays(deslocamentoDias),
                    LocalDate.parse(cabecalho.get("data_recebimento")).plusDays(deslocamentoDias));
            Instant recebidoEm = momento(origem.dataRecebimento(), LocalTime.of(9, 0));
            NotaFiscalEntrada nota = NotaFiscalEntrada.reconstituir(UUID.randomUUID(), origem,
                    assinatura(recebidoEm, recebidoEm));
            notaFiscalRepository.salvar(nota);

            List<String> numerosLote = new ArrayList<>();
            for (var linha : itens) {
                Material material = buscar(materiais, linha.get("material_codigo"), "material");
                String numero = Lote.normalizarNumero(linha.get("numero_lote"));
                LocalDate fabricacao = LocalDate.parse(linha.get("data_fabricacao")).plusDays(deslocamentoDias);
                LocalDate validade = LocalDate.parse(linha.get("data_validade")).plusDays(deslocamentoDias);

                LoteCarga lote = new LoteCarga(numero, material, null, origem, nota.getId(),
                        new BigDecimal(linha.get("quantidade")), fabricacao, validade,
                        StatusLote.valueOf(linha.get("status")), new BigDecimal(linha.get("saldo")), recebidoEm);
                if (lotes.putIfAbsent(refCompra(origem.notaFiscal(), numero), lote) != null) {
                    throw new IllegalStateException("Dataset inconsistente: lote repetido na nota "
                            + origem.notaFiscal() + ": " + numero);
                }
                numerosLote.add(numero);

                registrar(TipoEntidade.LOTE, lote.id, numero, AcaoAuditoria.ENTRADA_REGISTRADA, RESPONSAVEL,
                        recebidoEm,
                        Detalhes.com("numeroLote", numero)
                                .e("material", material.getCodigo())
                                .e("fornecedor", origem.fornecedor())
                                .e("notaFiscal", origem.notaFiscal())
                                .e("dataEmissaoNf", origem.dataEmissaoNf())
                                .e("dataRecebimento", origem.dataRecebimento())
                                .e("quantidade", lote.quantidade)
                                .e("unidadeDeMedida", material.getUnidadeDeMedida())
                                .e("dataFabricacao", fabricacao)
                                .e("dataValidade", validade));
            }
            registrar(TipoEntidade.NOTA_FISCAL, nota.getId(), nota.getNumero(), AcaoAuditoria.ENTRADA_REGISTRADA,
                    RESPONSAVEL, recebidoEm,
                    Detalhes.com("fornecedor", nota.getFornecedor())
                            .e("notaFiscal", nota.getNumero())
                            .e("dataEmissaoNf", nota.getDataEmissao())
                            .e("dataRecebimento", nota.getDataRecebimento())
                            .e("itens", itens.size())
                            .e("lotes", String.join("; ", numerosLote)));
        }
        return itensPorNota.size();
    }

    /**
     * Aplica as alocações em ordem cronológica: baixa o saldo de cada lote, registra a
     * genealogia e o evento LOTE_ALOCADO. Confere que cada consumo foi coberto por inteiro
     * e que o saldo final bate com o dataset.
     */
    private List<AlocacaoLote> carregarAlocacoes(Map<String, ConsumoCarga> consumos, Map<String, LoteCarga> lotes) {
        List<AlocacaoLote> alocacoes = new ArrayList<>();
        Map<UUID, BigDecimal> alocadoPorConsumo = new HashMap<>();
        for (var linha : LeitorCsv.ler(DIRETORIO + "alocacoes_lote.csv")) {
            ConsumoCarga carga = buscar(consumos,
                    chaveConsumo(linha.get("ordem_codigo"), linha.get("componente_codigo")), "consumo");
            String loteOrdem = linha.get("lote_ordem_codigo");
            LoteCarga lote = buscar(lotes, loteOrdem.isBlank()
                    ? refCompra(linha.get("lote_nota_fiscal"), Lote.normalizarNumero(linha.get("lote_numero")))
                    : refProducao(loteOrdem), "lote");
            BigDecimal quantidade = new BigDecimal(linha.get("quantidade"));
            if (!lote.material.getId().equals(carga.consumo().getMaterialId()) || quantidade.compareTo(lote.saldo) > 0) {
                throw new IllegalStateException("Dataset inconsistente: alocação inválida no lote " + lote.numero);
            }
            Instant quando = posterior(lote.criadoEm, carga.registradoEm());
            BigDecimal saldoAntes = lote.saldo;
            lote.saldo = lote.saldo.subtract(quantidade);
            lote.alteradoEm = posterior(lote.alteradoEm, quando);

            alocacoes.add(AlocacaoLote.reconstituir(UUID.randomUUID(), carga.consumo().getId(), lote.id,
                    quantidade, carga.responsavel(), quando));
            alocadoPorConsumo.merge(carga.consumo().getId(), quantidade, BigDecimal::add);
            carga.lotes().add(lote.numero + ": " + quantidade.stripTrailingZeros().toPlainString());

            Detalhes detalhes = Detalhes.com("ordemProducao", carga.ordem().getCodigo())
                    .e("material", carga.componente().getCodigo())
                    .e("quantidade", quantidade)
                    .e("unidadeDeMedida", lote.material.getUnidadeDeMedida())
                    .mudanca("saldo", saldoAntes, lote.saldo);
            if (lote.saldo.signum() == 0) {
                detalhes.mudanca("status", StatusLote.DISPONIVEL, StatusLote.CONSUMIDO);
            }
            registrar(TipoEntidade.LOTE, lote.id, lote.numero, AcaoAuditoria.LOTE_ALOCADO,
                    carga.responsavel(), quando, detalhes);
        }
        for (ConsumoCarga carga : consumos.values()) {
            BigDecimal consumida = carga.consumo().getQuantidadeConsumida();
            BigDecimal alocado = alocadoPorConsumo.getOrDefault(carga.consumo().getId(), BigDecimal.ZERO);
            if (consumida != null && consumida.compareTo(alocado) != 0) {
                throw new IllegalStateException("Dataset inconsistente: consumo de %s em %s sem alocação completa."
                        .formatted(carga.componente().getCodigo(), carga.ordem().getCodigo()));
            }
        }
        for (LoteCarga lote : lotes.values()) {
            if (lote.saldo.compareTo(lote.saldoFinal) != 0) {
                throw new IllegalStateException("Dataset inconsistente: saldo do lote " + lote.numero);
            }
        }
        return alocacoes;
    }

    private void registrarConsumos(Map<String, ConsumoCarga> consumos) {
        for (ConsumoCarga carga : consumos.values()) {
            ConsumoMaterial consumo = carga.consumo();
            if (consumo.getQuantidadeConsumida() == null) {
                continue;
            }
            registrar(TipoEntidade.ORDEM_PRODUCAO, carga.ordem().getId(), carga.ordem().getCodigo(),
                    AcaoAuditoria.CONSUMO_REGISTRADO, carga.responsavel(), carga.registradoEm(),
                    Detalhes.com("material", carga.componente().getCodigo())
                            .e("quantidadePlanejada", consumo.getQuantidadePlanejada())
                            .e("quantidadeConsumida", consumo.getQuantidadeConsumida())
                            .e("desvio", consumo.getDesvio())
                            .e("unidadeDeMedida", consumo.getUnidadeDeMedida())
                            .e("justificativa", consumo.getJustificativa())
                            .e("lotes", carga.lotes().isEmpty() ? null : String.join("; ", carga.lotes())));
        }
    }

    /** Status que não vêm do saldo (vencido, bloqueado) ganham evento próprio; depois grava. */
    private void salvarLotes(Map<String, LoteCarga> lotes) {
        for (LoteCarga lote : lotes.values()) {
            StatusLote status = lote.statusFinal;
            if (status == StatusLote.VENCIDO || status == StatusLote.BLOQUEADO) {
                Instant quando = posterior(lote.alteradoEm, status == StatusLote.VENCIDO
                        ? momento(lote.validade.plusDays(1), LocalTime.of(0, 5))
                        : momento(lote.fabricacao.plusDays(1), LocalTime.of(9, 0)));
                registrar(TipoEntidade.LOTE, lote.id, lote.numero, AcaoAuditoria.STATUS_ALTERADO, RESPONSAVEL,
                        quando, Detalhes.vazio().mudanca("status", StatusLote.DISPONIVEL, status));
                lote.alteradoEm = quando;
            }
            loteRepository.salvar(Lote.reconstituir(lote.id, lote.numero, lote.material.getId(),
                    lote.ordem == null ? null : lote.ordem.getId(), lote.origemCompra,
                    lote.notaFiscalId, lote.quantidade, lote.saldo, lote.material.getUnidadeDeMedida(),
                    lote.fabricacao, lote.validade, status,
                    new Assinatura(RESPONSAVEL, lote.criadoEm, RESPONSAVEL, lote.alteradoEm)));
        }
    }

    private static String chaveConsumo(String ordemCodigo, String componenteCodigo) {
        return ordemCodigo + "|" + componenteCodigo;
    }

    private static String refProducao(String ordemCodigo) {
        return "OP|" + ordemCodigo;
    }

    private static String refCompra(String notaFiscal, String numeroLote) {
        return "NF|" + notaFiscal + "|" + numeroLote;
    }

    /** Data e hora no fuso do relógio, limitadas a "agora": o histórico nunca fica no futuro. */
    private Instant momento(LocalDate data, LocalTime hora) {
        Instant instante = data.atTime(hora).atZone(relogio.getZone()).toInstant();
        return instante.isAfter(agora) ? agora : instante;
    }

    /** {@code candidato}, mas nunca antes de {@code anterior} — mantém a ordem cronológica. */
    private static Instant posterior(Instant anterior, Instant candidato) {
        return candidato.isBefore(anterior) ? anterior : candidato;
    }

    private static Assinatura assinatura(Instant criadoEm, Instant alteradoEm) {
        return new Assinatura(RESPONSAVEL, criadoEm, RESPONSAVEL, alteradoEm);
    }

    private void registrar(TipoEntidade tipo, UUID id, String referencia, AcaoAuditoria acao,
                           String usuario, Instant quando, Detalhes detalhes) {
        eventos.add(EventoAuditoria.registrar(tipo, id, referencia, acao, usuario, quando, detalhes.mapa()));
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

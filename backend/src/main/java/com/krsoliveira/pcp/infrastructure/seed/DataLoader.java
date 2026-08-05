package com.krsoliveira.pcp.infrastructure.seed;

import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Carrega as ordens de produção do CSV de carga inicial quando o perfil
 * {@code seed} está ativo ({@code --spring.profiles.active=seed}).
 *
 * <p>Estratégia: usa {@link OrdemProducao#reconstituir} para construir objetos
 * de domínio com status arbitrário (dados históricos não nascem PLANEJADOS).
 * Não chama o caso de uso {@code CriarOrdemProducao} de propósito: dados de
 * seed não passam pela validação de unicidade de código via repositório — a
 * idempotência é garantida pela verificação de contagem antes da carga.</p>
 *
 * <p>Fonte: {@code src/main/resources/dados/ordens_producao.csv}
 * (105 ordens de jan/2024 a out/2025, 8 centros de trabalho, 20 produtos).</p>
 */
@Component
@Profile("seed")
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final String CSV_PATH = "dados/ordens_producao.csv";

    private final OrdemProducaoRepository repositorio;

    public DataLoader(OrdemProducaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(String... args) throws Exception {
        if (repositorio.contarTodas() > 0) {
            log.info("DataLoader: banco já contém ordens — carga ignorada (idempotente).");
            return;
        }

        List<OrdemProducao> ordens = carregarCsv();
        for (OrdemProducao ordem : ordens) {
            repositorio.salvar(ordem);
        }
        log.info("DataLoader: {} ordens carregadas com sucesso.", ordens.size());
    }

    private List<OrdemProducao> carregarCsv() throws Exception {
        List<OrdemProducao> ordens = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(CSV_PATH);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String linha;
            boolean cabecalho = true;
            int numeroLinha = 0;

            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                if (cabecalho) {
                    cabecalho = false;
                    continue;
                }
                try {
                    ordens.add(parsearLinha(linha));
                } catch (Exception e) {
                    log.warn("DataLoader: linha {} ignorada por erro de parse — {}. Conteúdo: [{}]",
                            numeroLinha, e.getMessage(), linha);
                }
            }
        }
        return ordens;
    }

    private OrdemProducao parsearLinha(String linha) {
        // formato: codigo,produto,centro_de_trabalho,quantidade,inicio_planejado,fim_planejado,status
        String[] campos = linha.split(",", -1);
        if (campos.length != 7) {
            throw new IllegalArgumentException(
                    "esperados 7 campos, encontrados " + campos.length);
        }

        String codigo          = campos[0].trim();
        String produto         = campos[1].trim();
        String centroTrabalho  = campos[2].trim();
        int quantidade         = Integer.parseInt(campos[3].trim());
        LocalDate inicio       = LocalDate.parse(campos[4].trim());
        LocalDate fim          = LocalDate.parse(campos[5].trim());
        StatusOrdemProducao st = StatusOrdemProducao.valueOf(campos[6].trim());

        // criadaEm/atualizadaEm são metadados de rastreabilidade do sistema;
        // para dados históricos importados usamos o instante da carga.
        Instant agora = Instant.now();
        return OrdemProducao.reconstituir(
                UUID.randomUUID(), codigo, produto, centroTrabalho,
                quantidade, inicio, fim, st, agora, agora);
    }
}

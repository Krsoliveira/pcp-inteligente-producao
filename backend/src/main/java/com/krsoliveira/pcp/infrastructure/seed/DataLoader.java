package com.krsoliveira.pcp.infrastructure.seed;

import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * DataLoader de ordens de produção — DESATIVADO desde a Fase 5a (ADR-0007).
 *
 * O dataset anterior (ordens_producao.csv, 105 registros com campo {@code produto}
 * como VARCHAR) é incompatível com o novo modelo que usa {@code material_id} e
 * {@code lista_tecnica_id} como FKs.
 *
 * Será substituído por um script Python que gera um dataset sintético cobrindo
 * Material + BOM (Lista Técnica) + Lote + ConsumoMaterial, compatível com o
 * modelo expandido definido no ADR-0007.
 *
 * TODO (Fase 5b): implementar novo DataLoader com dataset sintético.
 */
@Component
@Profile("seed")
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final OrdemProducaoRepository repositorio;

    public DataLoader(OrdemProducaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(String... args) {
        log.warn("DataLoader: carga desativada na Fase 5a. Dataset ordens_producao.csv " +
                "é incompatível com o novo modelo (material_id + lista_tecnica_id). " +
                "Aguardando dataset sintético Python (TODO Fase 5b).");
    }
}

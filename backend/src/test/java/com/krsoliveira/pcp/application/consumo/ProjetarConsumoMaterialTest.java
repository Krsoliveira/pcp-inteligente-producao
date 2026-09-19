package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.lista.ListaTecnicaRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProjetarConsumoMaterialTest {

    private ListaTecnicaRepositoryEmMemoria listaTecnicaRepository;
    private ConsumoMaterialRepositoryEmMemoria consumoRepository;
    private ProjetarConsumoMaterial casoDeUso;

    private static final UUID MATERIAL_PA_ID = UUID.randomUUID();
    private static final UUID MATERIAL_MP1_ID = UUID.randomUUID();
    private static final UUID MATERIAL_MP2_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listaTecnicaRepository = new ListaTecnicaRepositoryEmMemoria();
        consumoRepository = new ConsumoMaterialRepositoryEmMemoria();
        casoDeUso = new ProjetarConsumoMaterial(consumoRepository, listaTecnicaRepository);
    }

    @Test
    @DisplayName("projeta consumos multiplicando quantidade de cada item pela quantidade da ordem")
    void projetaConsumosComMultiplicacao() {
        UUID ordemId = UUID.randomUUID();
        ListaTecnica lista = criarListaTecnicaComDoisItens();

        List<ConsumoMaterial> consumos = casoDeUso.executar(
                new ProjetarConsumoMaterial.Comando(ordemId, lista.getId(), 10));

        assertThat(consumos).hasSize(2);
        assertThat(consumos.get(0).getOrdemProducaoId()).isEqualTo(ordemId);
        assertThat(consumos.get(0).getQuantidadeConsumida()).isNull();

        // MP1: 2.5kg por unidade × 10 = 25kg
        ConsumoMaterial consumoMp1 = consumos.stream()
                .filter(c -> MATERIAL_MP1_ID.equals(c.getMaterialId()))
                .findFirst().orElseThrow();
        assertThat(consumoMp1.getQuantidadePlanejada())
                .isEqualByComparingTo(new BigDecimal("25.0"));

        // MP2: 0.5kg por unidade × 10 = 5kg
        ConsumoMaterial consumoMp2 = consumos.stream()
                .filter(c -> MATERIAL_MP2_ID.equals(c.getMaterialId()))
                .findFirst().orElseThrow();
        assertThat(consumoMp2.getQuantidadePlanejada())
                .isEqualByComparingTo(new BigDecimal("5.0"));
    }

    @Test
    @DisplayName("persiste todos os consumos no repositório")
    void persisteConsumosNoRepositorio() {
        UUID ordemId = UUID.randomUUID();
        ListaTecnica lista = criarListaTecnicaComDoisItens();

        casoDeUso.executar(new ProjetarConsumoMaterial.Comando(ordemId, lista.getId(), 5));

        assertThat(consumoRepository.listarPorOrdemProducao(ordemId)).hasSize(2);
    }

    private ListaTecnica criarListaTecnicaComDoisItens() {
        List<ItemListaTecnica> itens = List.of(
                ItemListaTecnica.criar(MATERIAL_MP1_ID, new BigDecimal("2.5"), "kg"),
                ItemListaTecnica.criar(MATERIAL_MP2_ID, new BigDecimal("0.5"), "kg"));
        ListaTecnica lista = ListaTecnica.criar(MATERIAL_PA_ID, "v1", itens);
        listaTecnicaRepository.salvar(lista);
        return lista;
    }
}

package com.krsoliveira.pcp.infrastructure.web.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Item inválido dentro de uma lista é recusado: a validação desce do pedido até cada item, e o campo vem no erro. */
class ValidacaoDasListasDosPedidosTest {

    private static final BigDecimal QUANTIDADE = BigDecimal.TEN;
    private static final List<RegistrarConsumoRequest.AlocacaoRequest> ALOCACAO_SEM_LOTE =
            List.of(new RegistrarConsumoRequest.AlocacaoRequest(null, QUANTIDADE));

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void criarValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        fabrica.close();
    }

    @Test
    @DisplayName("lista técnica: componente sem material")
    void itemDaListaTecnica() {
        var pedido = new CadastrarListaTecnicaRequest(UUID.randomUUID(), "1", null,
                List.of(new ItemListaTecnicaRequest(null, QUANTIDADE, "kg")));

        assertThat(camposRecusados(pedido)).containsExactly("itens[0].materialComponenteId");
    }

    @Test
    @DisplayName("nota fiscal: item sem material")
    void itemDaNotaFiscal() {
        LocalDate ontem = LocalDate.now().minusDays(1);
        var pedido = new RegistrarNotaFiscalRequest("Fornecedor", "NF-1", ontem, ontem,
                List.of(new RegistrarNotaFiscalRequest.Item(null, QUANTIDADE, "L-1", ontem, ontem.plusYears(1))));

        assertThat(camposRecusados(pedido)).containsExactly("itens[0].materialId");
    }

    @Test
    @DisplayName("registro de consumo: alocação sem lote")
    void alocacaoDoRegistro() {
        var pedido = new RegistrarConsumoRequest(QUANTIDADE, null, ALOCACAO_SEM_LOTE);

        assertThat(camposRecusados(pedido)).containsExactly("alocacoes[0].loteId");
    }

    @Test
    @DisplayName("item adicionado ao consumo: alocação sem lote")
    void alocacaoDoItemAdicionado() {
        var pedido = new AdicionarConsumoRequest(UUID.randomUUID(), QUANTIDADE, "Motivo", ALOCACAO_SEM_LOTE);

        assertThat(camposRecusados(pedido)).containsExactly("alocacoes[0].loteId");
    }

    @Test
    @DisplayName("lançamento: linha e item adicional inválidos")
    void linhaEItemDoLancamento() {
        var pedido = new LancarConsumoRequest(
                List.of(new LancarConsumoRequest.Linha(null, QUANTIDADE, null, List.of())),
                List.of(new LancarConsumoRequest.Item(null, QUANTIDADE, "Motivo", List.of())));

        assertThat(camposRecusados(pedido)).containsExactlyInAnyOrder("consumos[0].consumoId", "itensAdicionais[0].materialId");
    }

    @Test
    @DisplayName("lançamento: alocação sem lote, dentro da linha e dentro do item adicional")
    void alocacaoDoLancamento() {
        var pedido = new LancarConsumoRequest(
                List.of(new LancarConsumoRequest.Linha(UUID.randomUUID(), QUANTIDADE, null, ALOCACAO_SEM_LOTE)),
                List.of(new LancarConsumoRequest.Item(UUID.randomUUID(), QUANTIDADE, "Motivo", ALOCACAO_SEM_LOTE)));

        assertThat(camposRecusados(pedido)).containsExactlyInAnyOrder(
                "consumos[0].alocacoes[0].loteId", "itensAdicionais[0].alocacoes[0].loteId");
    }

    private static List<String> camposRecusados(Object pedido) {
        return validador.validate(pedido).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }
}

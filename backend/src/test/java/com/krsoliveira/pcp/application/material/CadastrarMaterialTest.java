package com.krsoliveira.pcp.application.material;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes do caso de uso com repositório em memória — sem Spring, sem banco.
 */
class CadastrarMaterialTest {

    private final MaterialRepositoryEmMemoria repositorio = new MaterialRepositoryEmMemoria();
    private final TrilhaDeAuditoriaEmMemoria trilha = new TrilhaDeAuditoriaEmMemoria();
    private final CadastrarMaterial casoDeUso = new CadastrarMaterial(repositorio, trilha.execucao());

    private UUID cadastrar(TipoMaterial tipo) {
        return casoDeUso.executar(new CadastrarMaterial.Comando("Motor elétrico 5CV", tipo, "un"));
    }

    private String codigo(UUID id) {
        return repositorio.buscarPorId(id).orElseThrow().getCodigo();
    }

    @Test
    @DisplayName("gera o primeiro código da faixa do tipo (103, 105, 110)")
    void geraPrimeiroCodigoDaFaixa() {
        assertThat(codigo(cadastrar(TipoMaterial.PRODUTO_ACABADO))).isEqualTo("103000001");
        assertThat(codigo(cadastrar(TipoMaterial.SEMIACABADO))).isEqualTo("105000001");
        assertThat(codigo(cadastrar(TipoMaterial.MATERIA_PRIMA))).isEqualTo("110000001");
    }

    @Test
    @DisplayName("cada tipo segue a própria sequência")
    void sequenciaPorTipo() {
        cadastrar(TipoMaterial.MATERIA_PRIMA);
        cadastrar(TipoMaterial.PRODUTO_ACABADO);

        assertThat(codigo(cadastrar(TipoMaterial.MATERIA_PRIMA))).isEqualTo("110000002");
        assertThat(codigo(cadastrar(TipoMaterial.PRODUTO_ACABADO))).isEqualTo("103000002");
    }

    @Test
    @DisplayName("assina o material e registra o evento CRIADO na trilha de auditoria")
    void assinaERegistraEvento() {
        UUID id = cadastrar(TipoMaterial.PRODUTO_ACABADO);

        var material = repositorio.buscarPorId(id).orElseThrow();
        assertThat(material.getAssinatura().criadoPor()).isEqualTo(USUARIO_TESTE);
        assertThat(trilha.eventos()).singleElement().satisfies(evento -> {
            assertThat(evento.getTipoEntidade()).isEqualTo(TipoEntidade.MATERIAL);
            assertThat(evento.getEntidadeId()).isEqualTo(id);
            assertThat(evento.getReferencia()).isEqualTo("103000001");
            assertThat(evento.getAcao()).isEqualTo(AcaoAuditoria.CRIADO);
            assertThat(evento.getUsuario()).isEqualTo(USUARIO_TESTE);
            assertThat(evento.getDetalhes()).containsEntry("tipo", "PRODUTO_ACABADO");
        });
    }

    @Test
    @DisplayName("tipo é obrigatório e a falha não registra evento")
    void tipoObrigatorio() {
        assertThatThrownBy(() -> cadastrar(null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("tipo");
        assertThat(trilha.eventos()).isEmpty();
    }
}

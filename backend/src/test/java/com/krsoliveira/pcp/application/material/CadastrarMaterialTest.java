package com.krsoliveira.pcp.application.material;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
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

    private CadastrarMaterial.Comando comandoValido(String codigo) {
        return new CadastrarMaterial.Comando(
                codigo, "Motor elétrico 5CV", TipoMaterial.PRODUTO_ACABADO, "un");
    }

    @Test
    @DisplayName("cadastra e persiste material válido, retornando UUID")
    void cadastraEPersisteMaterial() {
        UUID id = casoDeUso.executar(comandoValido("MAT-001"));

        assertThat(id).isNotNull();
        assertThat(repositorio.buscarPorId(id)).isPresent();
        assertThat(repositorio.buscarPorId(id).get().getCodigo()).isEqualTo("MAT-001");
    }

    @Test
    @DisplayName("assina o material e registra o evento CRIADO na trilha de auditoria")
    void assinaERegistraEvento() {
        UUID id = casoDeUso.executar(comandoValido("MAT-001"));

        var material = repositorio.buscarPorId(id).orElseThrow();
        assertThat(material.getAssinatura().criadoPor()).isEqualTo(USUARIO_TESTE);
        assertThat(trilha.eventos()).singleElement().satisfies(evento -> {
            assertThat(evento.getTipoEntidade()).isEqualTo(TipoEntidade.MATERIAL);
            assertThat(evento.getEntidadeId()).isEqualTo(id);
            assertThat(evento.getReferencia()).isEqualTo("MAT-001");
            assertThat(evento.getAcao()).isEqualTo(AcaoAuditoria.CRIADO);
            assertThat(evento.getUsuario()).isEqualTo(USUARIO_TESTE);
            assertThat(evento.getDetalhes()).containsEntry("tipo", "PRODUTO_ACABADO");
        });
    }

    @Test
    @DisplayName("falha de negócio não registra evento")
    void falhaNaoRegistraEvento() {
        casoDeUso.executar(comandoValido("MAT-001"));

        assertThatThrownBy(() -> casoDeUso.executar(comandoValido("MAT-001")));
        assertThat(trilha.eventos()).hasSize(1);
    }

    @Test
    @DisplayName("normaliza código em maiúsculas antes de verificar duplicata")
    void normalizaCodigoAntesDeVerificarDuplicata() {
        casoDeUso.executar(comandoValido("mat-001"));

        assertThatThrownBy(() -> casoDeUso.executar(comandoValido("MAT-001")))
                .isInstanceOf(CodigoMaterialJaUtilizadoException.class)
                .hasMessageContaining("MAT-001");
    }

    @Test
    @DisplayName("rejeita código duplicado com CodigoMaterialJaUtilizadoException")
    void rejeitaCodigoDuplicado() {
        casoDeUso.executar(comandoValido("MAT-001"));

        assertThatThrownBy(() -> casoDeUso.executar(comandoValido("MAT-001")))
                .isInstanceOf(CodigoMaterialJaUtilizadoException.class)
                .hasMessageContaining("MAT-001");
    }
}

package com.krsoliveira.pcp.application.material;

import com.krsoliveira.pcp.domain.material.TipoMaterial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes do caso de uso com repositório em memória — sem Spring, sem banco.
 */
class CadastrarMaterialTest {

    private final MaterialRepositoryEmMemoria repositorio = new MaterialRepositoryEmMemoria();
    private final CadastrarMaterial casoDeUso = new CadastrarMaterial(repositorio);

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

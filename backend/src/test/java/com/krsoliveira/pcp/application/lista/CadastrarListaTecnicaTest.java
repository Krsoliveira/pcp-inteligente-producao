package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadastrarListaTecnicaTest {

    private static final String USUARIO = TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;

    private ListaTecnicaRepositoryEmMemoria listaTecnicaRepository;
    private CadastrarListaTecnica useCase;
    private UUID materialId;
    private UUID outroMaterialId;

    @BeforeEach
    void setUp() {
        var materialRepository = new MaterialRepositoryEmMemoria();
        listaTecnicaRepository = new ListaTecnicaRepositoryEmMemoria();
        useCase = new CadastrarListaTecnica(listaTecnicaRepository, materialRepository,
                new TrilhaDeAuditoriaEmMemoria().execucao());
        materialId = UUID.randomUUID();
        outroMaterialId = UUID.randomUUID();
        materialRepository.salvar(Material.reconstituir(materialId, "PA-000001", "Produto A",
                TipoMaterial.PRODUTO_ACABADO, "UN", Assinatura.nova(USUARIO)));
        materialRepository.salvar(Material.reconstituir(outroMaterialId, "PA-000002", "Produto B",
                TipoMaterial.PRODUTO_ACABADO, "UN", Assinatura.nova(USUARIO)));
    }

    private UUID cadastrar(UUID material, String versao) {
        return useCase.executar(new CadastrarListaTecnica.Comando(material, versao,
                List.of(new CadastrarListaTecnica.ItemComando(UUID.randomUUID(), BigDecimal.ONE, "UN"))));
    }

    private ListaTecnica busca(UUID id) {
        return listaTecnicaRepository.buscarPorId(id).orElseThrow();
    }

    @Test
    @DisplayName("a primeira versão do material abre a lista (novo listaId)")
    void primeiraVersaoAbreALista() {
        ListaTecnica v1 = busca(cadastrar(materialId, "v1"));

        assertThat(v1.getListaId()).isNotNull().isNotEqualTo(v1.getId());
    }

    @Test
    @DisplayName("as novas versões do mesmo material entram na mesma lista")
    void versoesDoMesmoMaterialCompartilhamOListaId() {
        ListaTecnica v1 = busca(cadastrar(materialId, "v1"));
        ListaTecnica v2 = busca(cadastrar(materialId, "v2"));

        assertThat(v2.getListaId()).isEqualTo(v1.getListaId());
        assertThat(v2.getId()).isNotEqualTo(v1.getId());
    }

    @Test
    @DisplayName("materiais diferentes têm listas diferentes")
    void materiaisDiferentesTemListasDiferentes() {
        ListaTecnica deA = busca(cadastrar(materialId, "v1"));
        ListaTecnica deB = busca(cadastrar(outroMaterialId, "v1"));

        assertThat(deB.getListaId()).isNotEqualTo(deA.getListaId());
    }

    @Test
    @DisplayName("rejeita versão repetida no mesmo material, mas aceita o mesmo rótulo em outro material")
    void versaoRepetida() {
        cadastrar(materialId, "v1");

        assertThatThrownBy(() -> cadastrar(materialId, "v1")).isInstanceOf(VersaoListaTecnicaJaExisteException.class);
        assertThat(cadastrar(outroMaterialId, "v1")).isNotNull();
    }
}

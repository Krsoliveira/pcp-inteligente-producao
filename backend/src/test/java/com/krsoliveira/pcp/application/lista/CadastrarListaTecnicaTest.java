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
    private UUID semiacabadoId;
    private UUID componenteId;
    private UUID acabadoComoComponenteId;
    private MaterialRepositoryEmMemoria materialRepository;

    @BeforeEach
    void setUp() {
        materialRepository = new MaterialRepositoryEmMemoria();
        listaTecnicaRepository = new ListaTecnicaRepositoryEmMemoria();
        useCase = new CadastrarListaTecnica(listaTecnicaRepository, materialRepository,
                new TrilhaDeAuditoriaEmMemoria().execucao());
        materialId = UUID.randomUUID();
        outroMaterialId = UUID.randomUUID();
        semiacabadoId = UUID.randomUUID();
        componenteId = UUID.randomUUID();
        acabadoComoComponenteId = UUID.randomUUID();
        materialRepository.salvar(Material.reconstituir(materialId, "PA-000001", "Produto A",
                TipoMaterial.PRODUTO_ACABADO, "UN", Assinatura.nova(USUARIO)));
        materialRepository.salvar(Material.reconstituir(outroMaterialId, "PA-000002", "Produto B",
                TipoMaterial.PRODUTO_ACABADO, "UN", Assinatura.nova(USUARIO)));
        materialRepository.salvar(Material.reconstituir(semiacabadoId, "105000001", "Semiacabado S",
                TipoMaterial.SEMIACABADO, "UN", Assinatura.nova(USUARIO)));
        materialRepository.salvar(Material.reconstituir(componenteId, "110000001", "Chapa",
                TipoMaterial.MATERIA_PRIMA, "KG", Assinatura.nova(USUARIO)));
        materialRepository.salvar(Material.reconstituir(acabadoComoComponenteId, "103000009", "Produto Z",
                TipoMaterial.PRODUTO_ACABADO, "UN", Assinatura.nova(USUARIO)));
    }

    private UUID cadastrar(UUID material, String versao) {
        return useCase.executar(new CadastrarListaTecnica.Comando(material, versao,
                List.of(new CadastrarListaTecnica.ItemComando(componenteId, BigDecimal.ONE, "UN"))));
    }

    private CadastrarListaTecnica.Comando comando(UUID dono, UUID componente) {
        return new CadastrarListaTecnica.Comando(dono, "v1",
                List.of(new CadastrarListaTecnica.ItemComando(componente, BigDecimal.ONE, "UN")));
    }

    @Test
    @DisplayName("sem quantidade base a lista nasce com base 1; com base, ela é gravada")
    void quantidadeBase() {
        UUID semBase = useCase.executar(comando(materialId, componenteId));
        UUID comBase = useCase.executar(new CadastrarListaTecnica.Comando(materialId, "v2", new BigDecimal("100"),
                List.of(new CadastrarListaTecnica.ItemComando(componenteId, new BigDecimal("200"), "KG"))));

        assertThat(busca(semBase).getQuantidadeBase()).isEqualByComparingTo("1");
        assertThat(busca(comBase).getQuantidadeBase()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("base inválida é recusada e nada é salvo")
    void quantidadeBaseInvalida() {
        assertThatThrownBy(() -> useCase.executar(new CadastrarListaTecnica.Comando(materialId, "v1", BigDecimal.ZERO,
                List.of(new CadastrarListaTecnica.ItemComando(componenteId, BigDecimal.ONE, "KG")))))
                .isInstanceOf(com.krsoliveira.pcp.domain.RegraDeNegocioException.class).hasMessageContaining("quantidade base");
        assertThat(listaTecnicaRepository.listarPorMaterial(materialId)).isEmpty();
    }

    @Test
    @DisplayName("lista de produto acabado não aceita produto acabado como componente (regra inegociável, ADR-0024)")
    void produtoAcabadoNaoConsomeAcabado() {
        assertThatThrownBy(() -> useCase.executar(comando(materialId, acabadoComoComponenteId)))
                .isInstanceOf(com.krsoliveira.pcp.domain.RegraDeNegocioException.class)
                .hasMessageContaining("103000009").hasMessageContaining("só listas de semiacabado");
        assertThat(listaTecnicaRepository.listarPorMaterial(materialId)).isEmpty();
    }

    @Test
    @DisplayName("lista de semiacabado aceita produto acabado como componente (conversão)")
    void semiacabadoPodeConsumirAcabado() {
        UUID id = useCase.executar(comando(semiacabadoId, acabadoComoComponenteId));

        assertThat(busca(id).getItens()).hasSize(1);
    }

    @Test
    @DisplayName("componente que não existe é recusado")
    void componenteInexistente() {
        UUID fantasma = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.executar(comando(materialId, fantasma)))
                .isInstanceOf(com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException.class);
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

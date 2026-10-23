package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repositório de perfis em memória para os testes de caso de uso. Já nasce com os dois perfis de
 * sistema, como o banco depois da migração V13.
 */
public class PerfilAcessoRepositoryEmMemoria implements PerfilAcessoRepository {

    public static final UUID ID_ADMINISTRADOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID ID_VISUALIZADOR = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final List<PerfilAcesso> perfis = new ArrayList<>();

    public PerfilAcessoRepositoryEmMemoria() {
        perfis.add(PerfilAcesso.reconstituir(ID_ADMINISTRADOR, "Administrador", "Todas as permissões.",
                true, false, Set.of(), Assinatura.nova("sistema")));
        perfis.add(PerfilAcesso.reconstituir(ID_VISUALIZADOR, "Visualizador", "Só consulta.", false, true,
                Set.of(Permissao.DASHBOARD_VISUALIZAR, Permissao.LISTA_VISUALIZAR_ATIVAS), Assinatura.nova("sistema")));
    }

    @Override
    public PerfilAcesso salvar(PerfilAcesso perfil) {
        perfis.removeIf(p -> p.getId().equals(perfil.getId()));
        perfis.add(perfil);
        return perfil;
    }

    @Override
    public Optional<PerfilAcesso> buscarPorId(UUID id) {
        return perfis.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<PerfilAcesso> buscarPorNome(String nome) {
        return perfis.stream().filter(p -> p.getNome().equalsIgnoreCase(nome)).findFirst();
    }

    @Override
    public List<PerfilAcesso> listarTodos() {
        return perfis.stream().sorted(Comparator.comparing(p -> p.getNome().toLowerCase())).toList();
    }

    @Override
    public Optional<PerfilAcesso> buscarPadraoDoAutocadastro() {
        return perfis.stream().filter(PerfilAcesso::isPadraoDoAutocadastro).findFirst();
    }

    @Override
    public Optional<PerfilAcesso> buscarAdministrador() {
        return perfis.stream().filter(PerfilAcesso::isAdministrador).findFirst();
    }

    @Override
    public void excluir(UUID id) {
        perfis.removeIf(p -> p.getId().equals(id));
    }
}

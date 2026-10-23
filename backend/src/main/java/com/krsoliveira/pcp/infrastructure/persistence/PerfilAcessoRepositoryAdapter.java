package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta {@link PerfilAcessoRepository} com Spring Data JPA. */
@Repository
public class PerfilAcessoRepositoryAdapter implements PerfilAcessoRepository {

    private final PerfilAcessoSpringDataRepository springData;

    public PerfilAcessoRepositoryAdapter(PerfilAcessoSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public PerfilAcesso salvar(PerfilAcesso perfil) {
        return springData.save(PerfilAcessoJpaEntity.de(perfil)).paraDominio();
    }

    @Override
    public Optional<PerfilAcesso> buscarPorId(UUID id) {
        return springData.findById(id).map(PerfilAcessoJpaEntity::paraDominio);
    }

    @Override
    public Optional<PerfilAcesso> buscarPorNome(String nome) {
        return springData.findByNomeIgnoreCase(nome).map(PerfilAcessoJpaEntity::paraDominio);
    }

    @Override
    public List<PerfilAcesso> listarTodos() {
        return springData.findAllByOrderByNomeAsc().stream().map(PerfilAcessoJpaEntity::paraDominio).toList();
    }

    @Override
    public Optional<PerfilAcesso> buscarPadraoDoAutocadastro() {
        return springData.findByPadraoDoAutocadastroTrue().map(PerfilAcessoJpaEntity::paraDominio);
    }

    @Override
    public Optional<PerfilAcesso> buscarAdministrador() {
        return springData.findByAdministradorTrue().map(PerfilAcessoJpaEntity::paraDominio);
    }

    @Override
    public void excluir(UUID id) {
        springData.deleteById(id);
    }
}

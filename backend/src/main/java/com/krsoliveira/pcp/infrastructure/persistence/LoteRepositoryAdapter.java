package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ADAPTADOR: implementa a porta {@link LoteRepository} usando JPA/PostgreSQL.
 *
 * O método {@link #proximoSequencial} usa JPQL para contar lotes existentes
 * com o mesmo prefixo e retorna o próximo número sequencial.
 */
@Repository
public class LoteRepositoryAdapter implements LoteRepository {

    private final LoteSpringDataRepository springData;

    public LoteRepositoryAdapter(LoteSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public Lote salvar(Lote lote) {
        return springData.save(LoteJpaEntity.deDominio(lote)).paraDominio();
    }

    @Override
    public Optional<Lote> buscarPorId(UUID id) {
        return springData.findById(id).map(LoteJpaEntity::paraDominio);
    }

    @Override
    public Optional<Lote> buscarPorIdParaAtualizar(UUID id) {
        return springData.buscarParaAtualizar(id).map(LoteJpaEntity::paraDominio);
    }

    @Override
    public List<Lote> buscarPorIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return springData.findAllById(ids).stream().map(LoteJpaEntity::paraDominio).toList();
    }

    @Override
    public List<Lote> listarPorOrdens(Collection<UUID> ordemProducaoIds) {
        if (ordemProducaoIds.isEmpty()) return List.of();
        return springData.findByOrdemProducaoIdIn(ordemProducaoIds).stream()
                .map(LoteJpaEntity::paraDominio).toList();
    }

    @Override
    public List<Lote> listarDisponiveisPorMaterial(UUID materialId) {
        return springData.disponiveisPorMaterial(materialId).stream()
                .map(LoteJpaEntity::paraDominio).toList();
    }

    @Override
    public List<Lote> listarTodos() {
        return springData.findAll().stream()
                .map(LoteJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public boolean existeEntrada(UUID materialId, String fornecedor, String notaFiscal) {
        return springData.existsByMaterialIdAndFornecedorAndNotaFiscal(
                materialId, fornecedor, notaFiscal);
    }

    @Override
    public List<Lote> listarPorOrdemProducao(UUID ordemProducaoId) {
        return springData.findByOrdemProducaoId(ordemProducaoId).stream()
                .map(LoteJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public Optional<Lote> buscarPorNumeroLote(String numeroLote) {
        return springData.findByNumeroLote(numeroLote).map(LoteJpaEntity::paraDominio);
    }

    @Override
    public int proximoSequencial(UUID materialId, String prefixo) {
        return springData.proximoSequencial(prefixo);
    }
}

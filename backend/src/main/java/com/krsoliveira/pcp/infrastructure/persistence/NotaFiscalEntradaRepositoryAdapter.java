package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntrada;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntradaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** ADAPTADOR: implementa a porta {@link NotaFiscalEntradaRepository} com JPA/PostgreSQL. */
@Repository
public class NotaFiscalEntradaRepositoryAdapter implements NotaFiscalEntradaRepository {

    private final NotaFiscalEntradaSpringDataRepository springData;

    public NotaFiscalEntradaRepositoryAdapter(NotaFiscalEntradaSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public void salvar(NotaFiscalEntrada nota) {
        springData.save(NotaFiscalEntradaJpaEntity.deDominio(nota));
    }

    @Override
    public Optional<NotaFiscalEntrada> buscarPorId(UUID id) {
        return springData.findById(id).map(NotaFiscalEntradaJpaEntity::paraDominio);
    }

    @Override
    public boolean existe(String fornecedor, String numero) {
        return springData.existe(fornecedor, numero);
    }

    @Override
    public List<NotaFiscalEntrada> listarPorRecebimento(LocalDate de, LocalDate ate) {
        return springData.porRecebimento(de, ate).stream().map(NotaFiscalEntradaJpaEntity::paraDominio).toList();
    }
}

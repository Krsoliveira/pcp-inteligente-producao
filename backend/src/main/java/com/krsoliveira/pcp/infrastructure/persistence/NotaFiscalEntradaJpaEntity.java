package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.lote.OrigemCompra;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntrada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Espelho da tabela {@code nota_fiscal_entrada}. */
@Entity
@Table(name = "nota_fiscal_entrada")
public class NotaFiscalEntradaJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String fornecedor;

    @Column(nullable = false, length = 44)
    private String numero;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_recebimento", nullable = false)
    private LocalDate dataRecebimento;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "criado_por", nullable = false, length = 150)
    private String criadoPor;

    @Column(name = "atualizado_por", nullable = false, length = 150)
    private String atualizadoPor;

    protected NotaFiscalEntradaJpaEntity() {}

    public static NotaFiscalEntradaJpaEntity deDominio(NotaFiscalEntrada nota) {
        NotaFiscalEntradaJpaEntity e = new NotaFiscalEntradaJpaEntity();
        e.id = nota.getId();
        e.fornecedor = nota.getFornecedor();
        e.numero = nota.getNumero();
        e.dataEmissao = nota.getDataEmissao();
        e.dataRecebimento = nota.getDataRecebimento();
        Assinatura a = nota.getAssinatura();
        e.criadoEm = a.criadoEm();
        e.atualizadoEm = a.alteradoEm();
        e.criadoPor = a.criadoPor();
        e.atualizadoPor = a.alteradoPor();
        return e;
    }

    public NotaFiscalEntrada paraDominio() {
        return NotaFiscalEntrada.reconstituir(id,
                new OrigemCompra(fornecedor, numero, dataEmissao, dataRecebimento),
                new Assinatura(criadoPor, criadoEm, atualizadoPor, atualizadoEm));
    }
}

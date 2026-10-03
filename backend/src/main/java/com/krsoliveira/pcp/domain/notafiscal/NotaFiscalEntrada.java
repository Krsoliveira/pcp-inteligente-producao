package com.krsoliveira.pcp.domain.notafiscal;

import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.lote.OrigemCompra;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Nota fiscal de entrada de matéria-prima (ADR-0012): o cabeçalho do recebimento —
 * fornecedor, número, emissão e recebimento — e quem lançou.
 *
 * Os itens da nota são os próprios lotes de compra: cada lote aponta para a nota que o
 * trouxe. Da nota se chega a todos os lotes; de cada lote, à nota. É um registro
 * imutável: correções exigirão estorno.
 */
public final class NotaFiscalEntrada {

    private final UUID id;
    private final OrigemCompra dados;
    private final Assinatura assinatura;

    private NotaFiscalEntrada(UUID id, OrigemCompra dados, Assinatura assinatura) {
        this.id = id;
        this.dados = dados;
        this.assinatura = assinatura;
    }

    /** Nova nota. {@link OrigemCompra} valida fornecedor, número e as datas. */
    public static NotaFiscalEntrada registrar(String fornecedor, String numero, LocalDate dataEmissao,
                                              LocalDate dataRecebimento, String usuario) {
        return new NotaFiscalEntrada(UUID.randomUUID(),
                new OrigemCompra(fornecedor, numero, dataEmissao, dataRecebimento), Assinatura.nova(usuario));
    }

    public static NotaFiscalEntrada reconstituir(UUID id, OrigemCompra dados, Assinatura assinatura) {
        return new NotaFiscalEntrada(id, dados, assinatura);
    }

    public UUID getId() { return id; }
    /** Fornecedor, número e datas — o mesmo valor que vai para cada lote da nota. */
    public OrigemCompra getDados() { return dados; }
    public String getFornecedor() { return dados.fornecedor(); }
    public String getNumero() { return dados.notaFiscal(); }
    public LocalDate getDataEmissao() { return dados.dataEmissaoNf(); }
    public LocalDate getDataRecebimento() { return dados.dataRecebimento(); }
    public Assinatura getAssinatura() { return assinatura; }
    public Instant getRegistradaEm() { return assinatura.criadoEm(); }
}

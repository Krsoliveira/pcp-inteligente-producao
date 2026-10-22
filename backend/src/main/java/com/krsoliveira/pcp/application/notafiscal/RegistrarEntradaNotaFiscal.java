package com.krsoliveira.pcp.application.notafiscal;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntrada;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntradaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: registrar a entrada de uma nota fiscal de compra (ADR-0012).
 *
 * Orquestração (atômica e auditada):
 * 1. A mesma nota (fornecedor + número) não entra duas vezes.
 * 2. Cada item é uma matéria-prima com o lote do fornecedor (até 20 caracteres); o mesmo
 *    lote do mesmo material não pode repetir na nota nem já existir para o fornecedor.
 * 3. Grava a nota e um lote DISPONIVEL por item, ligado à nota — é a entrada no estoque.
 * 4. Registra na trilha a nota (com os lotes) e a entrada de cada lote.
 */
public class RegistrarEntradaNotaFiscal {

    private final NotaFiscalEntradaRepository notaRepository;
    private final LoteRepository loteRepository;
    private final MaterialRepository materialRepository;
    private final ExecucaoAuditada execucao;

    public RegistrarEntradaNotaFiscal(NotaFiscalEntradaRepository notaRepository,
                                      LoteRepository loteRepository,
                                      MaterialRepository materialRepository,
                                      ExecucaoAuditada execucao) {
        this.notaRepository = notaRepository;
        this.loteRepository = loteRepository;
        this.materialRepository = materialRepository;
        this.execucao = execucao;
    }

    /** Um item da nota: material, quantidade e o lote do fornecedor com suas datas. */
    public record Item(UUID materialId, BigDecimal quantidade, String numeroLote,
                       LocalDate dataFabricacao, LocalDate dataValidade) {}

    public record Comando(String fornecedor, String numero, LocalDate dataEmissao,
                          LocalDate dataRecebimento, List<Item> itens) {

        public Comando {
            itens = itens == null ? List.of() : List.copyOf(itens);
        }
    }

    public record Resultado(NotaFiscalEntrada nota, List<Lote> lotes) {}

    public Resultado executar(Comando comando) {
        return execucao.executar(ctx -> {
            NotaFiscalEntrada nota = NotaFiscalEntrada.registrar(comando.fornecedor(), comando.numero(),
                    comando.dataEmissao(), comando.dataRecebimento(), ctx.usuario());
            if (notaRepository.existe(nota.getFornecedor(), nota.getNumero())) {
                throw new NotaFiscalDuplicadaException(nota.getNumero(), nota.getFornecedor());
            }
            if (comando.itens().isEmpty()) {
                throw new RegraDeNegocioException("A nota fiscal precisa de pelo menos um item.");
            }
            notaRepository.salvar(nota);

            Set<String> lotesDaNota = new HashSet<>();
            List<Lote> lotes = new ArrayList<>();
            List<String> resumo = new ArrayList<>();
            int posicao = 0;
            for (Item item : comando.itens()) {
                posicao++;
                Material material = materialDoItem(item, posicao);
                String numeroLote = Lote.normalizarNumero(item.numeroLote());
                if (!lotesDaNota.add(material.getId() + "|" + numeroLote)) {
                    throw new RegraDeNegocioException("Item %d: o lote %s de %s se repete na nota."
                            .formatted(posicao, numeroLote, material.getCodigo()));
                }
                if (loteRepository.existeLote(material.getId(), numeroLote, nota.getFornecedor())) {
                    throw new LoteDuplicadoException(numeroLote, material.getCodigo(), nota.getFornecedor());
                }

                Lote lote = loteRepository.salvar(Lote.receberCompra(numeroLote, material.getId(),
                        nota.getDados(), nota.getId(), item.quantidade(), material.getUnidadeDeMedida(),
                        item.dataFabricacao(), item.dataValidade(), ctx.usuario()));
                lotes.add(lote);
                resumo.add("%s: %s %s (lote %s)".formatted(material.getCodigo(),
                        lote.getQuantidade().stripTrailingZeros().toPlainString(),
                        lote.getUnidadeDeMedida(), lote.getNumeroLote()));

                ctx.registrar(TipoEntidade.LOTE, lote.getId(), lote.getNumeroLote(),
                        AcaoAuditoria.ENTRADA_REGISTRADA,
                        Detalhes.com("numeroLote", lote.getNumeroLote())
                                .e("material", material.getCodigo())
                                .e("fornecedor", nota.getFornecedor())
                                .e("notaFiscal", nota.getNumero())
                                .e("dataEmissaoNf", nota.getDataEmissao())
                                .e("dataRecebimento", nota.getDataRecebimento())
                                .e("quantidade", lote.getQuantidade())
                                .e("unidadeDeMedida", lote.getUnidadeDeMedida())
                                .e("dataFabricacao", lote.getDataFabricacao())
                                .e("dataValidade", lote.getDataValidade()));
            }

            ctx.registrar(TipoEntidade.NOTA_FISCAL, nota.getId(), nota.getNumero(),
                    AcaoAuditoria.ENTRADA_REGISTRADA,
                    Detalhes.com("fornecedor", nota.getFornecedor())
                            .e("notaFiscal", nota.getNumero())
                            .e("dataEmissaoNf", nota.getDataEmissao())
                            .e("dataRecebimento", nota.getDataRecebimento())
                            .e("itens", lotes.size())
                            .e("lotes", String.join("; ", resumo)));
            return new Resultado(nota, List.copyOf(lotes));
        });
    }

    private Material materialDoItem(Item item, int posicao) {
        if (item.materialId() == null) {
            throw new RegraDeNegocioException("Item %d: informe o material.".formatted(posicao));
        }
        Material material = materialRepository.buscarPorId(item.materialId())
                .orElseThrow(() -> new MaterialNaoEncontradoException(item.materialId()));
        if (material.getTipo() != TipoMaterial.MATERIA_PRIMA) {
            throw new RegraDeNegocioException(
                    "Item %d: somente matéria-prima entra por nota fiscal; %s é %s."
                            .formatted(posicao, material.getCodigo(), material.getTipo()));
        }
        return material;
    }
}

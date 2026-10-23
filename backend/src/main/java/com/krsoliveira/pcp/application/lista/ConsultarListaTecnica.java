package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.comum.PermissoesDoUsuarioAtual;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.lista.FiltroListasTecnicas;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: consultar listas técnicas (individual ou por material).
 *
 * Autorização por objeto (ADR-0020): quem só tem {@code LISTA_VISUALIZAR_ATIVAS} enxerga apenas as
 * versões ativas; as demais são tratadas como inexistentes (404), sem revelar que existem.
 */
public class ConsultarListaTecnica {

    private final ListaTecnicaRepository listaTecnicaRepository;
    private final PermissoesDoUsuarioAtual permissoesAtuais;

    public ConsultarListaTecnica(ListaTecnicaRepository listaTecnicaRepository, PermissoesDoUsuarioAtual permissoesAtuais) {
        this.listaTecnicaRepository = listaTecnicaRepository;
        this.permissoesAtuais = permissoesAtuais;
    }

    public ListaTecnica buscarPorId(UUID id) {
        return listaTecnicaRepository.buscarPorId(id)
                .filter(this::visivel)
                .orElseThrow(() -> new ListaTecnicaNaoEncontradaException(id));
    }

    public List<ListaTecnica> listarTodas() {
        return listaTecnicaRepository.listarTodas().stream().filter(this::visivel).toList();
    }

    public Pagina<ListaTecnica> pesquisar(FiltroListasTecnicas filtro, PedidoDePagina pedido) {
        if (veTodas()) {
            return listaTecnicaRepository.pesquisar(filtro, pedido);
        }
        if (filtro.status() != null && filtro.status() != StatusListaTecnica.ATIVA) {
            return new Pagina<>(List.of(), pedido.pagina(), pedido.tamanho(), 0);
        }
        return listaTecnicaRepository.pesquisar(
                new FiltroListasTecnicas(filtro.materialId(), StatusListaTecnica.ATIVA), pedido);
    }

    public List<ListaTecnica> listarPorMaterial(UUID materialId) {
        return listaTecnicaRepository.listarPorMaterial(materialId).stream().filter(this::visivel).toList();
    }

    private boolean veTodas() {
        return permissoesAtuais.permissoes().contains(Permissao.LISTA_VISUALIZAR_TODAS);
    }

    private boolean visivel(ListaTecnica lista) {
        return veTodas() || lista.getStatus() == StatusListaTecnica.ATIVA;
    }
}

package com.krsoliveira.pcp.application.comum;

import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.EventoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.auditoria.TrilhaDeAuditoria;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Executa um caso de uso de escrita com rastreabilidade:
 * <ol>
 *   <li>identifica o usuário responsável ({@link UsuarioAtual});</li>
 *   <li>executa a operação dentro de uma {@link Transacao};</li>
 *   <li>grava, na mesma transação, os eventos de auditoria que a operação registrou.</li>
 * </ol>
 * Se a operação ou a gravação dos eventos falhar, nada é persistido.
 */
public class ExecucaoAuditada {

    private final UsuarioAtual usuarioAtual;
    private final Transacao transacao;
    private final TrilhaDeAuditoria trilha;

    public ExecucaoAuditada(UsuarioAtual usuarioAtual, Transacao transacao, TrilhaDeAuditoria trilha) {
        this.usuarioAtual = usuarioAtual;
        this.transacao = transacao;
        this.trilha = trilha;
    }

    public <T> T executar(Function<Contexto, T> operacao) {
        return executarComo(usuarioAtual.identificador(), operacao);
    }

    /**
     * Executa com um responsável explícito — para operações sem usuário logado, como o
     * autocadastro (a própria pessoa) ou processos do sistema (carga inicial, administrador
     * inicial).
     */
    public <T> T executarComo(String responsavel, Function<Contexto, T> operacao) {
        return transacao.executar(() -> {
            Contexto contexto = new Contexto(responsavel);
            T resultado = operacao.apply(contexto);
            if (!contexto.eventos.isEmpty()) {
                trilha.registrar(contexto.eventos);
            }
            return resultado;
        });
    }

    /** O que a operação enxerga: quem está agindo e onde registrar o que aconteceu. */
    public static final class Contexto {

        private final String usuario;
        private final List<EventoAuditoria> eventos = new ArrayList<>();

        private Contexto(String usuario) {
            this.usuario = usuario;
        }

        public String usuario() {
            return usuario;
        }

        public void registrar(TipoEntidade tipo, UUID entidadeId, String referencia,
                              AcaoAuditoria acao, Detalhes detalhes) {
            eventos.add(EventoAuditoria.registrar(tipo, entidadeId, referencia, acao, usuario,
                    Instant.now(), detalhes == null ? null : detalhes.mapa()));
        }
    }
}

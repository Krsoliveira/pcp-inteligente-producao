package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import com.krsoliveira.pcp.domain.usuario.PoliticaDeSenha;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

/**
 * Caso de uso: o próprio usuário troca a senha (ADR-0021). Vale no primeiro acesso, quando a senha
 * provisória precisa ser trocada, e a qualquer momento ("Alterar minha senha"). Exige a senha atual.
 *
 * A senha atual errada é uma regra de negócio violada (422), não um 401: o frontend desloga em 401 e o
 * usuário perderia o que digitou. A trilha registra o fato, nunca a senha nem o hash.
 */
public class TrocarMinhaSenha {

    private final UsuarioRepository usuarioRepository;
    private final CodificadorDeSenha codificador;
    private final ExecucaoAuditada execucao;

    public TrocarMinhaSenha(UsuarioRepository usuarioRepository, CodificadorDeSenha codificador, ExecucaoAuditada execucao) {
        this.usuarioRepository = usuarioRepository;
        this.codificador = codificador;
        this.execucao = execucao;
    }

    public void executar(String senhaAtual, String novaSenha) {
        execucao.executar(ctx -> {
            Usuario usuario = usuarioRepository.porEmail(ctx.usuario())
                    .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));

            if (senhaAtual == null || !codificador.verificar(senhaAtual, usuario.getSenhaHash())) {
                throw new RegraDeNegocioException("A senha atual não confere.");
            }
            PoliticaDeSenha.validar(novaSenha);
            if (codificador.verificar(novaSenha, usuario.getSenhaHash())) {
                throw new RegraDeNegocioException("A nova senha deve ser diferente da atual.");
            }

            boolean eraProvisoria = usuario.isDeveTrocarSenha();
            usuario.trocarSenha(codificador.codificar(novaSenha));
            usuarioRepository.salvar(usuario);
            ctx.registrar(TipoEntidade.USUARIO, usuario.getId(), usuario.getEmail(), AcaoAuditoria.SENHA_ALTERADA,
                    Detalhes.com("eraProvisoria", eraProvisoria));
            return null;
        });
    }
}

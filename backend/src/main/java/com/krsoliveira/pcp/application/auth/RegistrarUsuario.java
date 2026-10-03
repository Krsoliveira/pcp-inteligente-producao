package com.krsoliveira.pcp.application.auth;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import com.krsoliveira.pcp.domain.usuario.Perfil;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

/**
 * Caso de uso: registrar um usuário. Como não há ninguém logado nesse momento, o
 * responsável é informado explicitamente: a própria pessoa no autocadastro, ou um
 * identificador de sistema na criação do administrador inicial.
 */
public class RegistrarUsuario {

    private final UsuarioRepository usuarioRepository;
    private final CodificadorDeSenha codificadorDeSenha;
    private final ExecucaoAuditada execucao;

    public RegistrarUsuario(UsuarioRepository usuarioRepository,
                            CodificadorDeSenha codificadorDeSenha,
                            ExecucaoAuditada execucao) {
        this.usuarioRepository = usuarioRepository;
        this.codificadorDeSenha = codificadorDeSenha;
        this.execucao = execucao;
    }

    /** Autocadastro: a própria pessoa é a responsável pela criação da conta. */
    public Usuario autocadastrar(String nome, String email, String senhaPlana, Perfil perfil) {
        return executar(nome, email, senhaPlana, perfil, normalizar(email));
    }

    public Usuario executar(String nome, String email, String senhaPlana, Perfil perfil,
                            String responsavel) {
        return execucao.executarComo(responsavel, ctx -> {
            String emailNormalizado = normalizar(email);
            if (usuarioRepository.porEmail(emailNormalizado).isPresent()) {
                throw new EmailJaUtilizadoException(email);
            }
            String senhaHash = codificadorDeSenha.codificar(senhaPlana);
            Usuario usuario = usuarioRepository.salvar(
                    Usuario.criar(nome, emailNormalizado, senhaHash, perfil));
            ctx.registrar(TipoEntidade.USUARIO, usuario.getId(), usuario.getEmail(),
                    AcaoAuditoria.USUARIO_REGISTRADO,
                    Detalhes.com("email", usuario.getEmail()).e("perfil", usuario.getPerfil()));
            return usuario;
        });
    }

    private static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}

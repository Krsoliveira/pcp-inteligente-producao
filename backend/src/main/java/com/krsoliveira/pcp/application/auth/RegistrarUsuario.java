package com.krsoliveira.pcp.application.auth;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import com.krsoliveira.pcp.domain.acesso.AcessoNegadoException;
import com.krsoliveira.pcp.domain.usuario.PoliticaDeSenha;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

/**
 * Caso de uso: registrar um usuário. Como não há ninguém logado nesse momento, o
 * responsável é informado explicitamente: a própria pessoa no autocadastro, ou um
 * identificador de sistema na criação do administrador inicial. O autocadastro recebe sempre o
 * perfil padrão (de menor poder); só um gestor autenticado escolhe outro (ADR-0020).
 */
public class RegistrarUsuario {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final CodificadorDeSenha codificadorDeSenha;
    private final ExecucaoAuditada execucao;
    private final boolean autocadastroHabilitado;

    public RegistrarUsuario(UsuarioRepository usuarioRepository,
                            PerfilAcessoRepository perfilRepository,
                            CodificadorDeSenha codificadorDeSenha,
                            ExecucaoAuditada execucao) {
        this(usuarioRepository, perfilRepository, codificadorDeSenha, execucao, true);
    }

    public RegistrarUsuario(UsuarioRepository usuarioRepository,
                            PerfilAcessoRepository perfilRepository,
                            CodificadorDeSenha codificadorDeSenha,
                            ExecucaoAuditada execucao,
                            boolean autocadastroHabilitado) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.codificadorDeSenha = codificadorDeSenha;
        this.execucao = execucao;
        this.autocadastroHabilitado = autocadastroHabilitado;
    }

    /**
     * Autocadastro: a própria pessoa é a responsável pela criação da conta, que recebe o perfil
     * padrão do autocadastro. Desligado (ADR-0031), recusa antes de olhar os dados: a resposta é a mesma para qualquer
     * e-mail, e não dá para descobrir quem tem conta.
     */
    public Usuario autocadastrar(String nome, String email, String senhaPlana) {
        if (!autocadastroHabilitado) {
            throw new AcessoNegadoException("O cadastro público está desligado. Peça o seu acesso a um gestor.");
        }
        PoliticaDeSenha.validar(senhaPlana);
        PerfilAcesso padrao = perfilRepository.buscarPadraoDoAutocadastro()
                .orElseThrow(() -> new IllegalStateException("Perfil padrão do autocadastro não encontrado."));
        return executar(nome, email, senhaPlana, padrao, normalizar(email));
    }

    public Usuario executar(String nome, String email, String senhaPlana, PerfilAcesso perfil,
                            String responsavel) {
        return registrar(nome, email, senhaPlana, perfil, responsavel, false);
    }

    /**
     * Cria o usuário com uma senha provisória (um gestor a definiu): ele precisa trocá-la no primeiro acesso
     * (ADR-0021).
     */
    public Usuario executarComSenhaProvisoria(String nome, String email, String senhaPlana, PerfilAcesso perfil,
                                              String responsavel) {
        return registrar(nome, email, senhaPlana, perfil, responsavel, true);
    }

    private Usuario registrar(String nome, String email, String senhaPlana, PerfilAcesso perfil,
                              String responsavel, boolean senhaProvisoria) {
        return execucao.executarComo(responsavel, ctx -> {
            String emailNormalizado = normalizar(email);
            if (usuarioRepository.porEmail(emailNormalizado).isPresent()) {
                throw new EmailJaUtilizadoException(email);
            }
            String senhaHash = codificadorDeSenha.codificar(senhaPlana);
            Usuario usuario = usuarioRepository.salvar(senhaProvisoria
                    ? Usuario.criarComSenhaProvisoria(nome, emailNormalizado, senhaHash, perfil.getId())
                    : Usuario.criar(nome, emailNormalizado, senhaHash, perfil.getId()));
            ctx.registrar(TipoEntidade.USUARIO, usuario.getId(), usuario.getEmail(),
                    AcaoAuditoria.USUARIO_REGISTRADO,
                    Detalhes.com("email", usuario.getEmail()).e("perfil", perfil.getNome()));
            return usuario;
        });
    }

    private static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}

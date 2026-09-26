package com.krsoliveira.pcp.infrastructure.config;

import com.krsoliveira.pcp.application.auth.RegistrarUsuario;
import com.krsoliveira.pcp.domain.usuario.Perfil;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o usuário administrador (perfil GERENTE) na inicialização, a partir das
 * variáveis de ambiente {@code ADMIN_NOME}, {@code ADMIN_EMAIL} e {@code ADMIN_SENHA}.
 *
 * <ul>
 *   <li>Credenciais nunca ficam no código nem no Git — só no ambiente.</li>
 *   <li>Sem {@code ADMIN_EMAIL}, não faz nada.</li>
 *   <li>Idempotente: se o e-mail já existe, não altera a conta. Em especial, NÃO promove
 *       uma conta existente — senão quem se autocadastrasse antes com esse e-mail
 *       ganharia o perfil GERENTE com a própria senha.</li>
 *   <li>Configuração incompleta (e-mail sem nome ou senha) impede a aplicação de subir:
 *       falha rápida em vez de um administrador silenciosamente ausente.</li>
 * </ul>
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);
    private static final int SENHA_MINIMA = 8;

    private final UsuarioRepository usuarioRepository;
    private final RegistrarUsuario registrarUsuario;
    private final String nome;
    private final String email;
    private final String senha;

    public AdministradorInicial(UsuarioRepository usuarioRepository,
                                RegistrarUsuario registrarUsuario,
                                @Value("${app.admin.nome:}") String nome,
                                @Value("${app.admin.email:}") String email,
                                @Value("${app.admin.senha:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.registrarUsuario = registrarUsuario;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalStateException("ADMIN_EMAIL definido sem ADMIN_NOME.");
        }
        if (senha == null || senha.length() < SENHA_MINIMA) {
            throw new IllegalStateException(
                    "ADMIN_SENHA ausente ou com menos de %d caracteres.".formatted(SENHA_MINIMA));
        }

        String emailNormalizado = email.trim().toLowerCase();
        if (usuarioRepository.porEmail(emailNormalizado).isPresent()) {
            log.info("Administrador inicial: e-mail já cadastrado — nenhuma alteração feita.");
            return;
        }
        registrarUsuario.executar(nome, emailNormalizado, senha, Perfil.GERENTE);
        // Não registra e-mail nem senha no log (dados pessoais / segredo).
        log.info("Administrador inicial criado com perfil GERENTE.");
    }
}

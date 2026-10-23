package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.acesso.AtribuirPerfilAoUsuario;
import com.krsoliveira.pcp.application.acesso.AtualizarPerfilAcesso;
import com.krsoliveira.pcp.application.acesso.ConsultarAcessos;
import com.krsoliveira.pcp.application.acesso.ConsultarUsuarioLogado;
import com.krsoliveira.pcp.application.acesso.CriarPerfilAcesso;
import com.krsoliveira.pcp.application.acesso.CriarUsuarioPorGestor;
import com.krsoliveira.pcp.application.acesso.ExcluirPerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.infrastructure.web.dto.AtribuirPerfilRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.CriarUsuarioRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.PerfilAcessoRequest;
import com.krsoliveira.pcp.infrastructure.web.dto.PerfilAcessoResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.PermissaoResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.UsuarioAcessoResponse;
import com.krsoliveira.pcp.infrastructure.web.dto.UsuarioLogadoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Gestão de acessos (ADR-0020): catálogo de permissões, perfis e usuários. Cada operação exige a
 * permissão correspondente no servidor; as regras de "só se concede o que se tem" ficam nos casos de uso.
 */
@RestController
@RequestMapping("/api/v1/acessos")
@Tag(name = "Acessos", description = "Perfis de acesso, permissões e usuários")
public class AcessoController {

    private final ConsultarAcessos consultarAcessos;
    private final ConsultarUsuarioLogado consultarUsuarioLogado;
    private final CriarPerfilAcesso criarPerfil;
    private final AtualizarPerfilAcesso atualizarPerfil;
    private final ExcluirPerfilAcesso excluirPerfil;
    private final CriarUsuarioPorGestor criarUsuario;
    private final AtribuirPerfilAoUsuario atribuirPerfil;

    public AcessoController(ConsultarAcessos consultarAcessos, ConsultarUsuarioLogado consultarUsuarioLogado,
                            CriarPerfilAcesso criarPerfil, AtualizarPerfilAcesso atualizarPerfil,
                            ExcluirPerfilAcesso excluirPerfil, CriarUsuarioPorGestor criarUsuario,
                            AtribuirPerfilAoUsuario atribuirPerfil) {
        this.consultarAcessos = consultarAcessos;
        this.consultarUsuarioLogado = consultarUsuarioLogado;
        this.criarPerfil = criarPerfil;
        this.atualizarPerfil = atualizarPerfil;
        this.excluirPerfil = excluirPerfil;
        this.criarUsuario = criarUsuario;
        this.atribuirPerfil = atribuirPerfil;
    }

    @GetMapping("/eu")
    @Operation(summary = "Quem sou eu e o que posso fazer",
            description = "Devolve o usuário logado, o perfil e as permissões. A tela usa para mostrar ou esconder "
                    + "telas e botões; a barreira de verdade é o servidor, em cada endpoint.")
    public UsuarioLogadoResponse eu() {
        return UsuarioLogadoResponse.de(consultarUsuarioLogado.executar());
    }

    @GetMapping("/permissoes")
    @PreAuthorize("hasAuthority('ACESSO_VISUALIZAR')")
    @Operation(summary = "Catálogo de permissões", description = "Todas as permissões que existem, com a área de cada uma.")
    public List<PermissaoResponse> permissoes() {
        return consultarAcessos.permissoes().stream().map(PermissaoResponse::de).toList();
    }

    @GetMapping("/perfis")
    @PreAuthorize("hasAuthority('ACESSO_VISUALIZAR')")
    @Operation(summary = "Lista os perfis de acesso", description = "Por nome, com as permissões e a quantidade de usuários de cada um.")
    public List<PerfilAcessoResponse> listarPerfis() {
        return consultarAcessos.listarPerfis().stream().map(PerfilAcessoResponse::de).toList();
    }

    @GetMapping("/perfis/{id}")
    @PreAuthorize("hasAuthority('ACESSO_VISUALIZAR')")
    @Operation(summary = "Busca um perfil de acesso")
    public PerfilAcessoResponse buscarPerfil(@PathVariable UUID id) {
        return PerfilAcessoResponse.de(consultarAcessos.buscarPerfil(id));
    }

    @PostMapping("/perfis")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    @Operation(summary = "Cria um perfil de acesso",
            description = "Só se concede o que se tem: o perfil não pode ter permissões que quem cria não possui (403).")
    public ResponseEntity<PerfilAcessoResponse> criarPerfil(@Valid @RequestBody PerfilAcessoRequest request) {
        PerfilAcesso criado = criarPerfil.executar(request.nome(), request.descricao(), request.permissoes());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.getId()).toUri();
        return ResponseEntity.created(local).body(PerfilAcessoResponse.de(criado, 0));
    }

    @PutMapping("/perfis/{id}")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    @Operation(summary = "Altera nome, descrição e permissões de um perfil",
            description = "O perfil Administrador não pode ser alterado (422). Quem altera precisa ter todas as "
                    + "permissões do perfil, antes e depois (403).")
    public PerfilAcessoResponse atualizarPerfil(@PathVariable UUID id, @Valid @RequestBody PerfilAcessoRequest request) {
        atualizarPerfil.executar(id, request.nome(), request.descricao(), request.permissoes());
        return PerfilAcessoResponse.de(consultarAcessos.buscarPerfil(id));
    }

    @DeleteMapping("/perfis/{id}")
    @PreAuthorize("hasAuthority('PERFIL_GERENCIAR')")
    @Operation(summary = "Exclui um perfil de acesso",
            description = "Só perfis que não são de sistema e que nenhum usuário usa (422).")
    public ResponseEntity<Void> excluirPerfil(@PathVariable UUID id) {
        excluirPerfil.executar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('ACESSO_VISUALIZAR')")
    @Operation(summary = "Lista os usuários e o perfil de cada um")
    public List<UsuarioAcessoResponse> listarUsuarios() {
        return consultarAcessos.listarUsuarios().stream().map(UsuarioAcessoResponse::de).toList();
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasAuthority('USUARIO_GERENCIAR')")
    @Operation(summary = "Cria um usuário com o perfil escolhido",
            description = "O gestor define a senha provisória. O perfil não pode ter permissões que o gestor não possui (403).")
    public ResponseEntity<UsuarioAcessoResponse> criarUsuario(@Valid @RequestBody CriarUsuarioRequest request) {
        Usuario criado = criarUsuario.executar(request.nome(), request.email(), request.senhaProvisoria(), request.perfilId());
        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.getId()).toUri();
        return ResponseEntity.created(local).body(
                UsuarioAcessoResponse.de(new ConsultarAcessos.UsuarioComPerfil(criado,
                        consultarAcessos.buscarPerfil(criado.getPerfilId()).perfil())));
    }

    @PatchMapping("/usuarios/{id}/perfil")
    @PreAuthorize("hasAuthority('USUARIO_GERENCIAR')")
    @Operation(summary = "Troca o perfil de um usuário",
            description = "Ninguém altera o próprio perfil (403); só se concede o que se tem (403); o último "
                    + "Administrador não pode sair do perfil (422).")
    public UsuarioAcessoResponse atribuirPerfil(@PathVariable UUID id, @Valid @RequestBody AtribuirPerfilRequest request) {
        Usuario salvo = atribuirPerfil.executar(id, request.perfilId());
        return UsuarioAcessoResponse.de(new ConsultarAcessos.UsuarioComPerfil(salvo,
                consultarAcessos.buscarPerfil(salvo.getPerfilId()).perfil()));
    }
}

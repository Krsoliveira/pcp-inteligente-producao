package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.acesso.Permissao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Corpo da criação e da alteração de um perfil.
 *
 * @param versao só na alteração, e opcional: a versão do perfil que a tela leu. Se não for mais a atual, a alteração
 *               é recusada com 409 (ADR-0034). Na criação é ignorada.
 */
public record PerfilAcessoRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 60, message = "Nome deve ter no máximo 60 caracteres")
        String nome,

        @Size(max = 200, message = "Descrição deve ter no máximo 200 caracteres")
        String descricao,

        @NotEmpty(message = "Marque ao menos uma permissão")
        List<Permissao> permissoes,

        @PositiveOrZero(message = "Versão inválida")
        Long versao
) {}

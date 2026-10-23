package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.acesso.Permissao;

/** Uma "caixinha" do catálogo: o identificador, a área em que aparece e o texto para o usuário. */
public record PermissaoResponse(String permissao, String area, String descricao) {

    public static PermissaoResponse de(Permissao p) {
        return new PermissaoResponse(p.name(), p.getArea(), p.getDescricao());
    }
}

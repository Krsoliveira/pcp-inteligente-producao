package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.auditoria.Pagina;

import java.util.List;
import java.util.function.Function;

public record PaginaResponse<T>(List<T> itens, int pagina, int tamanho, long total, int totalPaginas) {

    public static <D, T> PaginaResponse<T> de(Pagina<D> pagina, Function<D, T> conversor) {
        return new PaginaResponse<>(pagina.itens().stream().map(conversor).toList(),
                pagina.pagina(), pagina.tamanho(), pagina.total(), pagina.totalPaginas());
    }
}

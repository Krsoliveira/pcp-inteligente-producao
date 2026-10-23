package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.acesso.PerfilNaoEncontradoException;
import com.krsoliveira.pcp.application.acesso.PerfilNomeJaUtilizadoException;
import com.krsoliveira.pcp.application.acesso.UsuarioNaoEncontradoException;
import com.krsoliveira.pcp.application.auth.EmailJaUtilizadoException;
import com.krsoliveira.pcp.application.consumo.ConsumoMaterialNaoEncontradoException;
import com.krsoliveira.pcp.application.lista.ListaTecnicaNaoEncontradaException;
import com.krsoliveira.pcp.application.lista.VersaoListaTecnicaJaExisteException;
import com.krsoliveira.pcp.application.notafiscal.LoteDuplicadoException;
import com.krsoliveira.pcp.application.notafiscal.NotaFiscalDuplicadaException;
import com.krsoliveira.pcp.application.notafiscal.NotaFiscalNaoEncontradaException;
import com.krsoliveira.pcp.application.lote.LoteNaoEncontradoException;
import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.application.ordem.CodigoJaUtilizadoException;
import com.krsoliveira.pcp.application.ordem.NomeTipoOrdemJaUtilizadoException;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoNaoEncontradaException;
import com.krsoliveira.pcp.application.ordem.TipoOrdemNaoEncontradoException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.acesso.AcessoNegadoException;
import com.krsoliveira.pcp.infrastructure.security.CorpoGrandeDemaisException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte exceções em respostas HTTP padronizadas no formato Problem Details
 * (RFC 9457) — um JSON com {@code title}, {@code status} e {@code detail}.
 *
 * Mapa de erros:
 *  - 400: requisição malformada (falha de Bean Validation)
 *  - 404: recurso não encontrado
 *  - 409: conflito (código de ordem duplicado)
 *  - 413: corpo da requisição acima do limite (lido sem Content-Length)
 *  - 422: regra de negócio violada
 */
@RestControllerAdvice
public class TratadorGlobalDeErros {

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail regraDeNegocio(RegraDeNegocioException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problema.setTitle("Regra de negócio violada");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    /** Falta a permissão exigida pelo endpoint (segurança por método) ou a regra "só se concede o que se tem". */
    @ExceptionHandler({AccessDeniedException.class, AcessoNegadoException.class})
    public ProblemDetail acessoNegado(RuntimeException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problema.setTitle("Acesso negado");
        problema.setDetail(ex instanceof AcessoNegadoException
                ? ex.getMessage()
                : "Você não tem permissão para fazer isso.");
        return problema;
    }

    @ExceptionHandler({PerfilNaoEncontradoException.class, UsuarioNaoEncontradoException.class})
    public ProblemDetail acessoNaoEncontrado(RuntimeException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Recurso não encontrado");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(PerfilNomeJaUtilizadoException.class)
    public ProblemDetail perfilDuplicado(PerfilNomeJaUtilizadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Conflito");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(OrdemProducaoNaoEncontradaException.class)
    public ProblemDetail naoEncontrada(OrdemProducaoNaoEncontradaException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Recurso não encontrado");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(CodigoJaUtilizadoException.class)
    public ProblemDetail codigoDuplicado(CodigoJaUtilizadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Conflito");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(EmailJaUtilizadoException.class)
    public ProblemDetail emailDuplicado(EmailJaUtilizadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Conflito");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(MaterialNaoEncontradoException.class)
    public ProblemDetail materialNaoEncontrado(MaterialNaoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Recurso não encontrado");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(ListaTecnicaNaoEncontradaException.class)
    public ProblemDetail listaTecnicaNaoEncontrada(ListaTecnicaNaoEncontradaException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Recurso não encontrado");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler({NotaFiscalDuplicadaException.class, LoteDuplicadoException.class})
    public ProblemDetail entradaDuplicada(RuntimeException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Conflito");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(NotaFiscalNaoEncontradaException.class)
    public ProblemDetail notaFiscalNaoEncontrada(NotaFiscalNaoEncontradaException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Recurso não encontrado");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(VersaoListaTecnicaJaExisteException.class)
    public ProblemDetail versaoListaDuplicada(VersaoListaTecnicaJaExisteException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Conflito");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(ConsumoMaterialNaoEncontradoException.class)
    public ProblemDetail consumoNaoEncontrado(ConsumoMaterialNaoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Recurso não encontrado");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(LoteNaoEncontradoException.class)
    public ProblemDetail loteNaoEncontrado(LoteNaoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Recurso não encontrado");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(TipoOrdemNaoEncontradoException.class)
    public ProblemDetail tipoOrdemNaoEncontrado(TipoOrdemNaoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Recurso não encontrado");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(NomeTipoOrdemJaUtilizadoException.class)
    public ProblemDetail nomeTipoOrdemDuplicado(NomeTipoOrdemJaUtilizadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Conflito");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail credenciaisInvalidas(BadCredentialsException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problema.setTitle("Credenciais inválidas");
        problema.setDetail("E-mail ou senha incorretos.");
        return problema;
    }

    @ExceptionHandler(CorpoGrandeDemaisException.class)
    public ProblemDetail corpoGrandeDemais(CorpoGrandeDemaisException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.PAYLOAD_TOO_LARGE);
        problema.setTitle("Conteúdo grande demais");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail requisicaoInvalida(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.put(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Requisição inválida");
        problema.setDetail("Um ou mais campos são inválidos.");
        problema.setProperty("erros", erros);
        return problema;
    }
}

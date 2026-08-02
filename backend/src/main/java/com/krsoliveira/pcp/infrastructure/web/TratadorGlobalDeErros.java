package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.auth.EmailJaUtilizadoException;
import com.krsoliveira.pcp.application.ordem.CodigoJaUtilizadoException;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoNaoEncontradaException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail credenciaisInvalidas(BadCredentialsException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problema.setTitle("Credenciais inválidas");
        problema.setDetail("E-mail ou senha incorretos.");
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

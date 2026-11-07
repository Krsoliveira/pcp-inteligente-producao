package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.application.comum.ConflitoException;
import com.krsoliveira.pcp.application.comum.RecursoNaoEncontradoException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.acesso.AcessoNegadoException;
import com.krsoliveira.pcp.infrastructure.security.CorpoGrandeDemaisException;
import com.krsoliveira.pcp.infrastructure.security.UsuarioDetailsService;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte exceções em respostas HTTP padronizadas no formato Problem Details
 * (RFC 9457) — um JSON com {@code title}, {@code status} e {@code detail}.
 *
 * Mapa de erros:
 *  - 400: requisição malformada (falha de Bean Validation, valor de tipo errado, parâmetro ausente, JSON ilegível)
 *  - 404: recurso não encontrado
 *  - 405: método não permitido
 *  - 409: conflito (código de ordem duplicado)
 *  - 413: corpo da requisição acima do limite (lido sem Content-Length)
 *  - 422: regra de negócio violada
 */
@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler implements ResponseBodyAdvice<ProblemDetail> {

    /** Valor de {@code type} quando o erro não tem documentação própria (RFC 9457, seção 4.2.1). */
    private static final URI TIPO_PADRAO = URI.create("about:blank");

    /** Classe 22 do SQLSTATE, "exceção de dados": número acima da coluna, byte nulo em texto. */
    private static final String CLASSE_SQL_DADO_RECUSADO = "22";

    @Override
    public boolean supports(MethodParameter tipoDeRetorno, Class<? extends HttpMessageConverter<?>> conversor) {
        return ProblemDetail.class.isAssignableFrom(tipoDeRetorno.getParameterType());
    }

    /**
     * Mantém o {@code type} em toda resposta de erro. Até o Spring Framework 6 o {@code ProblemDetail} já nascia com
     * {@code about:blank}; no 7 o campo começa vazio e o JSON deixa de trazê-lo. O contrato de {@code /api/v1} só
     * cresce, então o campo continua presente (ADR-0030).
     */
    @Override
    public ProblemDetail beforeBodyWrite(ProblemDetail problema, MethodParameter tipoDeRetorno, MediaType tipoDeConteudo,
                                         Class<? extends HttpMessageConverter<?>> conversor,
                                         ServerHttpRequest requisicao, ServerHttpResponse resposta) {
        if (problema != null && problema.getType() == null) {
            problema.setType(TIPO_PADRAO);
        }
        return problema;
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail regraDeNegocio(RegraDeNegocioException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problema.setTitle("Regra de negócio violada");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail recursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Recurso não encontrado");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail conflito(ConflitoException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Conflito");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    /** Falta a permissão exigida pelo endpoint (segurança por método) ou a regra "só se concede o que se tem". */
    @ExceptionHandler({AccessDeniedException.class, AcessoNegadoException.class})
    public ProblemDetail acessoNegado(RuntimeException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problema.setTitle("Acesso negado");
        if (senhaProvisoriaPendente()) {
            problema.setDetail("Defina uma nova senha para continuar.");
            problema.setProperty("codigo", "TROCAR_SENHA");
            return problema;
        }
        problema.setDetail(ex instanceof AcessoNegadoException
                ? ex.getMessage()
                : "Você não tem permissão para fazer isso.");
        return problema;
    }

    /** O usuário logado ainda precisa trocar a senha provisória (ADR-0021)? */
    private static boolean senhaProvisoriaPendente() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.getAuthorities().stream()
                .anyMatch(a -> UsuarioDetailsService.SENHA_PROVISORIA.equals(a.getAuthority()));
    }

    /**
     * Valor que passou pela validação do corpo, mas o banco não aceita. É erro de quem chamou: responde 400, sem repetir o
     * valor. Os demais erros de acesso a dados (banco fora do ar, por exemplo) seguem como erro do sistema.
     */
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail dadoRecusadoPeloBanco(DataAccessException ex) {
        if (!(ex.getMostSpecificCause() instanceof SQLException erroSql) || erroSql.getSQLState() == null
                || !erroSql.getSQLState().startsWith(CLASSE_SQL_DADO_RECUSADO)) {
            throw ex;
        }
        logger.warn("Dado recusado pelo banco, SQLSTATE " + erroSql.getSQLState());
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Requisição inválida");
        problema.setDetail("Um dos valores informados está fora do limite ou do formato aceito.");
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
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONTENT_TOO_LARGE);
        problema.setTitle("Conteúdo grande demais");
        problema.setDetail(ex.getMessage());
        return problema;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders cabecalhos,
                                                                  HttpStatusCode status, WebRequest requisicao) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.put(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Requisição inválida");
        problema.setDetail("Um ou mais campos são inválidos.");
        problema.setProperty("erros", erros);
        return handleExceptionInternal(ex, problema, cabecalhos, status, requisicao);
    }

    /**
     * Erros do próprio Spring MVC (tipo errado, parâmetro ausente, JSON ilegível, método não permitido...). Antes saíam
     * no formato padrão do Spring ({@code timestamp}, {@code status}, {@code error}, {@code path}). Passam ao RFC 9457,
     * e os campos antigos continuam porque o contrato de {@code /api/v1} só cresce.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object corpo, HttpHeaders cabecalhos,
                                                             HttpStatusCode status, WebRequest requisicao) {
        ResponseEntity<Object> resposta = super.handleExceptionInternal(ex, corpo, cabecalhos, status, requisicao);
        if (resposta != null && resposta.getBody() instanceof ProblemDetail problema) {
            completarErroDoFramework(problema, ex, status, requisicao);
        }
        return resposta;
    }

    private static void completarErroDoFramework(ProblemDetail problema, Exception ex, HttpStatusCode status, WebRequest requisicao) {
        // O corpo chega aqui dentro de ResponseEntity, que o beforeBodyWrite acima não alcança.
        problema.setType(TIPO_PADRAO);
        problema.setTitle(tituloDoErro(status, problema.getTitle()));
        String mensagem = mensagemDoErro(ex);
        if (mensagem != null) {
            problema.setDetail(mensagem);
        }
        problema.setProperty("timestamp", Instant.now().toString());
        HttpStatus conhecido = HttpStatus.resolve(status.value());
        problema.setProperty("error", conhecido == null ? null : conhecido.getReasonPhrase());
        if (requisicao instanceof ServletWebRequest servlet) {
            problema.setProperty("path", servlet.getRequest().getRequestURI());
        }
    }

    private static String tituloDoErro(HttpStatusCode status, String padrao) {
        return switch (status.value()) {
            case 400 -> "Requisição inválida";
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método não permitido";
            default -> padrao;
        };
    }

    /** A mensagem não repete o valor enviado pelo cliente, só o nome do parâmetro, que vem do código. */
    private static String mensagemDoErro(Exception ex) {
        return switch (ex) {
            case MethodArgumentTypeMismatchException tipoErrado -> "O valor informado para '%s' é inválido.".formatted(tipoErrado.getName());
            case TypeMismatchException tipoErrado -> "Um dos valores informados é inválido.";
            case MissingServletRequestParameterException ausente -> "O parâmetro '%s' é obrigatório.".formatted(ausente.getParameterName());
            case HttpMessageNotReadableException ilegivel -> "O corpo da requisição não pôde ser lido. Confira o formato dos dados.";
            case HttpRequestMethodNotSupportedException metodo -> "Este método não é permitido neste endereço.";
            case NoResourceFoundException inexistente -> "O endereço pedido não existe.";
            default -> null;
        };
    }
}

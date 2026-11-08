package com.krsoliveira.pcp.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Registra, em log, os eventos de segurança que ajudam a investigar abuso: login, acesso negado e limite excedido.
 *
 * <p>Dado pessoal não vai para o log: o e-mail aparece mascarado ({@code a***@dominio.com}) e nunca se registra senha,
 * token nem corpo de requisição. O e-mail vem do cliente, então também é higienizado contra quebra de linha (injeção
 * de log). O {@code requestId} do MDC liga o evento à requisição.
 */
public final class EventosDeSeguranca {

    /** Logger com nome próprio, para filtrar só estes eventos na plataforma de logs. */
    private static final Logger LOG = LoggerFactory.getLogger("pcp.seguranca");

    private static final int TAMANHO_MAXIMO_DO_DOMINIO = 64;
    private static final Pattern CARACTERE_INVALIDO_NO_DOMINIO = Pattern.compile("[^A-Za-z0-9.-]");
    private static final Pattern CONTROLE = Pattern.compile("\\p{Cntrl}");
    private static final String SEM_USUARIO = "anonimo";

    private EventosDeSeguranca() {
    }

    public static void loginComSucesso(String email) {
        LOG.info("login_sucesso usuario={}", mascarar(email));
    }

    public static void loginRecusado(String email, String motivo) {
        LOG.warn("login_recusado usuario={} motivo={}", mascarar(email), motivo);
    }

    public static void acessoNegado(String usuario, String metodo, String caminho) {
        LOG.warn("acesso_negado usuario={} requisicao={} {}", mascarar(usuario), apenasImprimiveis(metodo),
                apenasImprimiveis(caminho));
    }

    public static void limiteExcedido(String metodo, String caminho) {
        LOG.warn("limite_excedido requisicao={} {}", apenasImprimiveis(metodo), apenasImprimiveis(caminho));
    }

    /** {@code maria@empresa.com} vira {@code m***@empresa.com}. Sem arroba reconhecível, não sobra nada do texto. */
    static String mascarar(String email) {
        if (email == null || email.isBlank()) {
            return SEM_USUARIO;
        }
        int arroba = email.indexOf('@');
        if (arroba < 1) {
            return "***";
        }
        String inicial = apenasImprimiveis(email.substring(0, 1));
        String dominio = CARACTERE_INVALIDO_NO_DOMINIO.matcher(email.substring(arroba + 1)).replaceAll("?");
        if (dominio.length() > TAMANHO_MAXIMO_DO_DOMINIO) {
            dominio = dominio.substring(0, TAMANHO_MAXIMO_DO_DOMINIO);
        }
        return inicial + "***@" + dominio;
    }

    /** Troca caracteres de controle (quebra de linha, tabulação) por "?" para o valor não forjar outra linha de log. */
    static String apenasImprimiveis(String valor) {
        return valor == null ? "" : CONTROLE.matcher(valor).replaceAll("?");
    }
}

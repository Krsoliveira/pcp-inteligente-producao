package com.krsoliveira.pcp.infrastructure.security;

/**
 * Lançada ao ler um corpo sem tamanho declarado (chunked) que passou do limite.
 * É RuntimeException de propósito: assim atravessa o parse do JSON sem virar
 * "corpo ilegível" (400) e o {@code TratadorGlobalDeErros} a converte em 413.
 */
public class CorpoGrandeDemaisException extends RuntimeException {

    public CorpoGrandeDemaisException(long limiteBytes) {
        super("O corpo da requisição excede o limite de " + limiteBytes + " bytes.");
    }
}

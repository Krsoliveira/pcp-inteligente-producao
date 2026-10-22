package com.krsoliveira.pcp.application.comum;

import java.util.function.Supplier;

/**
 * Porta: executa um bloco de forma atômica — ou tudo é gravado, ou nada.
 * Mantém os casos de uso livres de anotações de framework.
 */
@FunctionalInterface
public interface Transacao {

    <T> T executar(Supplier<T> bloco);

    /** Execução direta, sem transação real — para testes e cenários em memória. */
    static Transacao direta() {
        return new Transacao() {
            @Override
            public <T> T executar(Supplier<T> bloco) {
                return bloco.get();
            }
        };
    }
}

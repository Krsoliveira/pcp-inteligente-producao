package com.krsoliveira.pcp.infrastructure.config;

import com.krsoliveira.pcp.application.comum.Transacao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Implementação da porta {@link Transacao} com o gerenciador de transações do Spring. */
@Component
public class TransacaoSpring implements Transacao {

    private final TransactionTemplate template;

    public TransacaoSpring(PlatformTransactionManager gerenciador) {
        this.template = new TransactionTemplate(gerenciador);
    }

    @Override
    public <T> T executar(Supplier<T> bloco) {
        return template.execute(status -> bloco.get());
    }
}

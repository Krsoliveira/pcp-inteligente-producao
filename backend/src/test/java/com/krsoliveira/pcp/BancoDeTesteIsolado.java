package com.krsoliveira.pcp;

import org.springframework.test.context.ContextCustomizerFactories;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A classe de teste roda num banco PostgreSQL só dela, vazio e migrado pelo Flyway, dentro do contêiner compartilhado
 * pela suíte ({@link BancoDeTeste}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ContextCustomizerFactories(BancoDeTeste.Fabrica.class)
public @interface BancoDeTesteIsolado {
}
